package com.alarm.eagle.cep.operator;

import com.alarm.eagle.cep.processor.PatternProcessor;
import org.apache.flink.api.common.functions.util.FunctionUtils;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.common.typeutils.base.ListSerializer;
import org.apache.flink.api.common.typeutils.base.LongSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.cep.configuration.SharedBufferCacheConfig;
import org.apache.flink.cep.functions.PatternProcessFunction;
import org.apache.flink.cep.functions.TimedOutPartialMatchHandler;
import org.apache.flink.cep.nfa.NFA;
import org.apache.flink.cep.nfa.NFAState;
import org.apache.flink.cep.nfa.NFAStateSerializer;
import org.apache.flink.cep.nfa.aftermatch.AfterMatchSkipStrategy;
import org.apache.flink.cep.nfa.compiler.NFACompiler;
import org.apache.flink.cep.nfa.sharedbuffer.SharedBuffer;
import org.apache.flink.cep.nfa.sharedbuffer.SharedBufferAccessor;
import org.apache.flink.cep.time.TimerService;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.runtime.operators.coordination.OperatorEvent;
import org.apache.flink.runtime.operators.coordination.OperatorEventHandler;
import org.apache.flink.runtime.state.StateInitializationContext;
import org.apache.flink.runtime.state.VoidNamespace;
import org.apache.flink.runtime.state.VoidNamespaceSerializer;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.InternalTimer;
import org.apache.flink.streaming.api.operators.InternalTimerService;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.operators.Output;
import org.apache.flink.streaming.api.operators.TimestampedCollector;
import org.apache.flink.streaming.api.operators.Triggerable;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.runtime.tasks.StreamTask;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.apache.flink.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * ★★★ Dynamic CEP core operator (Phase 1 multi-rule parallel edition).
 *
 * <p>Differences from the MVP single-rule version:
 * <ul>
 *   <li>Single NFA → {@code Map<PatternProcessor, NFA>} multi-NFA parallel</li>
 *   <li>Single state set → per-rule independent state (named with {@code {name}-{id}-{version}} suffix)</li>
 *   <li>Per-rule independent TimerService</li>
 *   <li>Per-rule independent Metrics Counter</li>
 *   <li>The same event is evaluated in parallel by all active NFAs</li>
 * </ul>
 *
 * <p><b>Current limitation</b>: SharedBuffer is shared across rules (no actual conflict when different rules have different state names).
 *
 * @param <IN>  Input event type
 * @param <KEY> Key type
 * @param <OUT> Output type
 */
public class DynamicCepOperator<IN, KEY, OUT>
        extends AbstractStreamOperator<OUT>
        implements OneInputStreamOperator<IN, OUT>,
                   Triggerable<KEY, VoidNamespace>,
                   OperatorEventHandler {

    private static final Logger LOG =
            LoggerFactory.getLogger(DynamicCepOperator.class);
    private static final long serialVersionUID = 2L;

    // ======================== State Name Prefix Constants ========================

    private static final String NFA_STATE_PREFIX = "nfaState";
    private static final String EVENT_QUEUE_PREFIX = "eventQueue";
    private static final String TIMER_SERVICE_PREFIX = "watermark-callbacks";

    // ======================== Constructor Parameters ========================

    private final boolean isProcessingTime;
    private final TypeSerializer<IN> inputSerializer;

    // ======================== ★ Multi-Rule Data Structures (Core Changes) ========================

    /**
     * ★ Rule → NFA mapping. Each rule (id + version) corresponds to one NFA instance.
     * Replace this Map entirely on rule update.
     */
    private transient Map<RuleKey, RuleContext<IN, OUT>> ruleContexts;

    /**
     * ★ SharedBuffer — shared by all rules (current limitation).
     * Can be upgraded later to per-rule independent SharedBuffer (requires modifying Flink SharedBuffer source).
     */
    private transient SharedBuffer<IN> sharedPartialMatches;

    // ======================== Deferred State Cleanup (executed when key context is available) ========================

    /**
     * Marks which old rules need state cleanup (key = RuleKey).
     *
     * <p><b>Known limitation</b>: ValueState is per-key, processElement can only clean up the current key
     * state. Old state for other keys becomes orphaned, relying on Flink State TTL for async reclamation.
     * Can be upgraded later to use RawKeyedStateBackend for direct batch cleanup.
     */
    private transient final List<RuleKey> pendingStateClear = new ArrayList<>();

    /** ★ Paused rule IDs (skipped in processElement) */
    private transient final java.util.Set<String> pausedRuleIds = new java.util.HashSet<>();

    /** ★ Draining rule IDs (no longer accept new events, only process existing partial matches) */
    private transient final java.util.Set<String> drainingRuleIds = new java.util.HashSet<>();

    // ======================== Runtime Components ========================

    private transient TimestampedCollector<OUT> collector;
    private transient ContextFunctionImpl context;
    private transient StateInitializationContext initContext;

    public DynamicCepOperator(
            TypeSerializer<IN> inputSerializer,
            boolean isProcessingTime) {
        this.inputSerializer = Preconditions.checkNotNull(inputSerializer);
        this.isProcessingTime = isProcessingTime;
    }

    // ======================== Lifecycle ========================

    @Override
    public void setup(
            StreamTask<?, ?> containingTask,
            org.apache.flink.streaming.api.graph.StreamConfig config,
            Output<StreamRecord<OUT>> output) {
        super.setup(containingTask, config, output);
    }

    @Override
    public void initializeState(StateInitializationContext context)
            throws Exception {
        System.out.println("[CEP] initializeState() started...");
        super.initializeState(context);
        System.out.println("[CEP] super.initializeState() completed");
        this.initContext = context;

        // ★ SharedBuffer: shared by all rules (current limitation)
        this.sharedPartialMatches = new SharedBuffer<>(
                context.getKeyedStateStore(),
                inputSerializer,
                SharedBufferCacheConfig.of(
                        getOperatorConfig().getConfiguration()));

        this.ruleContexts = new HashMap<>();
        System.out.println("[CEP] initializeState() completed");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void open() throws Exception {
        System.out.println("[CEP] open() started...");
        super.open();
        System.out.println("[CEP] super.open() completed");

        collector = new TimestampedCollector<>(output);
        context = new ContextFunctionImpl();

        System.out.println("[CEP] open() completed (rules will be dynamically loaded upon receiving OperatorEvent)");
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (ruleContexts != null) {
            for (RuleContext<IN, OUT> rc : ruleContexts.values()) {
                closeRuleContext(rc);
            }
            ruleContexts.clear();
        }
        if (sharedPartialMatches != null) {
            sharedPartialMatches.releaseCacheStatisticsTimer();
        }
    }

    // ======================== Event Processing (core — broadcast event to all rules)====================

    @Override
    public void processElement(StreamRecord<IN> element) throws Exception {
        // ★ Deferred cleanup of old rule state (with key context)
        if (!pendingStateClear.isEmpty()) {
            for (RuleKey key : pendingStateClear) {
                RuleContext<IN, OUT> oldRc = ruleContexts.get(key);
                if (oldRc != null) {
                    oldRc.computationStates.clear();
                    oldRc.elementQueueState.clear();
                }
            }
            pendingStateClear.clear();
        }

        if (ruleContexts.isEmpty()) {
            System.out.println("[CEP] ⚠ Event arrived but no active rules, dropping and waiting...");
            return;
        }

        // ★ Each event is evaluated by all rules in parallel
        for (Map.Entry<RuleKey, RuleContext<IN, OUT>> entry :
                ruleContexts.entrySet()) {
            RuleKey ruleKey = entry.getKey();
            RuleContext<IN, OUT> rc = entry.getValue();

            try {
                processElementForRule(element, ruleKey, rc);
            } catch (Exception e) {
                LOG.error("Error processing event for rule {}-v{}: {}",
                        ruleKey.id, ruleKey.version, e.getMessage(), e);
            }
        }
    }

    /** Processing logic for a single event per rule */
    private void processElementForRule(
            StreamRecord<IN> element,
            RuleKey ruleKey,
            RuleContext<IN, OUT> rc) throws Exception {

        // ★ Phase 5: skip paused or draining rules
        if (shouldSkipRule(ruleKey.id)) {
            return;
        }

        if (isProcessingTime) {
            NFAState nfaState = getNFAState(rc);
            long timestamp = getProcessingTimeService()
                    .getCurrentProcessingTime();
            advanceTime(rc, nfaState, timestamp);
            processEvent(rc, nfaState, element.getValue(), timestamp);
            updateNFA(rc, nfaState);
        } else {
            long timestamp = element.getTimestamp();
            IN value = element.getValue();

            if (timestamp > rc.timerService.currentWatermark()) {
                bufferEvent(rc, value, timestamp);
            } else {
                rc.numLateRecordsDropped.inc();
            }
        }
    }

    // ======================== Timer Trigger (Iterate all rules) ========================

    @Override
    public void onEventTime(InternalTimer<KEY, VoidNamespace> timer)
            throws Exception {
        for (Map.Entry<RuleKey, RuleContext<IN, OUT>> entry :
                ruleContexts.entrySet()) {
            RuleContext<IN, OUT> rc = entry.getValue();
            processTimedEvents(rc, false);
        }
    }

    @Override
    public void onProcessingTime(InternalTimer<KEY, VoidNamespace> timer)
            throws Exception {
        for (Map.Entry<RuleKey, RuleContext<IN, OUT>> entry :
                ruleContexts.entrySet()) {
            RuleContext<IN, OUT> rc = entry.getValue();
            processTimedEvents(rc, true);
        }
    }

    private void processTimedEvents(RuleContext<IN, OUT> rc, boolean isProcTime)
            throws Exception {
        PriorityQueue<Long> sortedTimestamps = getSortedTimestamps(rc);
        NFAState nfaState = getNFAState(rc);

        while (!sortedTimestamps.isEmpty()
                && (isProcTime || sortedTimestamps.peek()
                        <= rc.timerService.currentWatermark())) {
            long timestamp = sortedTimestamps.poll();
            advanceTime(rc, nfaState, timestamp);
            List<IN> elements = rc.elementQueueState.get(timestamp);
            if (elements != null) {
                for (IN event : elements) {
                    processEvent(rc, nfaState, event, timestamp);
                }
            }
            rc.elementQueueState.remove(timestamp);
        }

        advanceTime(rc, nfaState,
                isProcTime ? rc.timerService.currentProcessingTime()
                        : rc.timerService.currentWatermark());
        updateNFA(rc, nfaState);
    }

    // ======================== Core Matching Logic (Consistent with MVP) ========================

    private void processEvent(
            RuleContext<IN, OUT> rc,
            NFAState nfaState, IN event, long timestamp) throws Exception {
        AfterMatchSkipStrategy skipStrategy = getAfterMatchSkipStrategy(rc);

        try (SharedBufferAccessor<IN> accessor =
                     sharedPartialMatches.getAccessor()) {
            Collection<Map<String, List<IN>>> patterns =
                    rc.nfa.process(accessor, nfaState, event, timestamp,
                            skipStrategy, rc.cepTimerService);

            if (rc.nfa.getWindowTime() > 0
                    && nfaState.isNewStartPartialMatch()) {
                registerTimer(rc,
                        timestamp + rc.nfa.getWindowTime());
            }
            processMatchedSequences(rc, patterns, timestamp);
        }
    }

    private void advanceTime(
            RuleContext<IN, OUT> rc,
            NFAState nfaState, long timestamp) throws Exception {
        AfterMatchSkipStrategy skipStrategy = getAfterMatchSkipStrategy(rc);

        try (SharedBufferAccessor<IN> accessor =
                     sharedPartialMatches.getAccessor()) {
            Tuple2<
                    Collection<Map<String, List<IN>>>,
                    Collection<Tuple2<Map<String, List<IN>>, Long>>
                    > result = rc.nfa.advanceTime(
                            accessor, nfaState, timestamp, skipStrategy);

            if (!result.f0.isEmpty()) {
                processMatchedSequences(rc, result.f0, timestamp);
            }
            if (!result.f1.isEmpty()) {
                processTimedOutSequences(rc, result.f1);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processMatchedSequences(
            RuleContext<IN, OUT> rc,
            Iterable<Map<String, List<IN>>> matchingSequences,
            long timestamp) throws Exception {
        setTimestamp(timestamp);
        for (Map<String, List<IN>> match : matchingSequences) {
            rc.patternMatchedTimes.inc();
            rc.userFunction.processMatch(
                    match, context, (Collector<OUT>) collector);
        }
    }

    @SuppressWarnings("unchecked")
    private void processTimedOutSequences(
            RuleContext<IN, OUT> rc,
            Collection<Tuple2<Map<String, List<IN>>, Long>> timedOut)
            throws Exception {
        if (rc.userFunction instanceof TimedOutPartialMatchHandler) {
            TimedOutPartialMatchHandler<IN> handler =
                    (TimedOutPartialMatchHandler<IN>) rc.userFunction;
            for (Tuple2<Map<String, List<IN>>, Long> match : timedOut) {
                setTimestamp(match.f1);
                handler.processTimedOutMatch(match.f0, context);
            }
        }
    }

    // ======================== State Read/Write(Per Rule) ========================

    private NFAState getNFAState(RuleContext<IN, OUT> rc) throws IOException {
        NFAState state = rc.computationStates.value();
        return state != null ? state : rc.nfa.createInitialNFAState();
    }

    private void updateNFA(RuleContext<IN, OUT> rc, NFAState state)
            throws IOException {
        if (state.isStateChanged()) {
            state.resetStateChanged();
            state.resetNewStartPartialMatch();
            rc.computationStates.update(state);
        }
    }

    // ======================== Event Buffering(Per Rule) ========================

    private void registerTimer(RuleContext<IN, OUT> rc, long timestamp) {
        if (isProcessingTime) {
            rc.timerService.registerProcessingTimeTimer(
                    VoidNamespace.INSTANCE, timestamp + 1);
        } else {
            rc.timerService.registerEventTimeTimer(
                    VoidNamespace.INSTANCE, timestamp);
        }
    }

    private void bufferEvent(RuleContext<IN, OUT> rc,
                             IN event, long currentTime) throws Exception {
        List<IN> elements = rc.elementQueueState.get(currentTime);
        if (elements == null) {
            elements = new ArrayList<>();
            registerTimer(rc, currentTime);
        }
        elements.add(event);
        rc.elementQueueState.put(currentTime, elements);
    }

    private PriorityQueue<Long> getSortedTimestamps(
            RuleContext<IN, OUT> rc) throws Exception {
        PriorityQueue<Long> sorted = new PriorityQueue<>();
        for (Long ts : rc.elementQueueState.keys()) {
            sorted.offer(ts);
        }
        return sorted;
    }

    private AfterMatchSkipStrategy getAfterMatchSkipStrategy(
            RuleContext<IN, OUT> rc) {
        if (rc.processor != null) {
            try {
                return Optional.ofNullable(
                        rc.processor.getPattern(getUserCodeClassloader())
                                .getAfterMatchSkipStrategy())
                        .orElse(AfterMatchSkipStrategy.noSkip());
            } catch (Exception e) {
                LOG.warn("Failed to get skip strategy, using noSkip.", e);
            }
        }
        return AfterMatchSkipStrategy.noSkip();
    }

    private void setTimestamp(long timestamp) {
        if (!isProcessingTime) {
            collector.setAbsoluteTimestamp(timestamp);
        }
        context.setTimestamp(timestamp);
    }

    // ======================== OperatorEvent Handling (Rule Hot Swap)====================

    @Override
    @SuppressWarnings("unchecked")
    public void handleOperatorEvent(OperatorEvent event) {
        try {
            if (event instanceof UpdatePatternProcessorEvent) {
                // ★ TM restart recovery: full replacement
                System.out.println("[CEP] Received full rule set event, total: "
                        + ((UpdatePatternProcessorEvent<IN>) event)
                            .getProcessors().size()
                        + "");
                handleFullUpdate((UpdatePatternProcessorEvent<IN>) event);
            } else if (event instanceof PatternProcessorChangeEvent) {
                // ★ incremental change: added / removed / updated
                PatternProcessorChangeEvent<IN> change =
                        (PatternProcessorChangeEvent<IN>) event;
                System.out.println("[CEP] Received incremental change event: +"
                        + change.getAdded().size() + " added, ~"
                        + change.getRemoved().size() + " removed, ↑"
                        + change.getUpdated().size() + " updated");
                handleIncrementalChange(change);
            } else if (event instanceof RuleLifecycleEvent) {
                // ★ Rule lifecycle control
                handleLifecycleEvent((RuleLifecycleEvent) event);
            } else {
                LOG.warn("Received unexpected operator event: {}", event);
            }
            System.out.println("[CEP] Event handling completed, current: " + ruleContexts.size()
                    + " rules active");
        } catch (Exception e) {
            System.err.println("[CEP] ★★★ handleOperatorEvent Exception!★★★");
            e.printStackTrace(System.err);
            throw new RuntimeException(
                    "Failed to update pattern processors.", e);
        }
    }

    /**
     * ★ Handle incremental change event (add/remove/update).
     * Only affects changed rules; other rules remain untouched.
     */
    @SuppressWarnings("unchecked")
    private void handleIncrementalChange(
            PatternProcessorChangeEvent<IN> change) throws Exception {

        // ① Handle removed (deletion)
        for (PatternProcessorChangeEvent.RuleIdentifier removed :
                change.getRemoved()) {
            RuleKey key = new RuleKey(removed.getId(), removed.getVersion());
            RuleContext<IN, OUT> oldRc = ruleContexts.get(key);
            if (oldRc != null) {
                pendingStateClear.add(key);
                closeRuleContext(oldRc);
                ruleContexts.remove(key);
                System.out.println("[CEP] Rule offline: " + key);
            }
        }

        // ② Handle updated (version upgrade)
        for (PatternProcessor<IN> processor : change.getUpdated()) {
            // Close old version
            RuleKey oldKey = findExistingRuleById(processor.getId());
            if (oldKey != null) {
                RuleContext<IN, OUT> oldRc = ruleContexts.get(oldKey);
                pendingStateClear.add(oldKey);
                closeRuleContext(oldRc);
                ruleContexts.remove(oldKey);
            }

            // Create new version
            RuleKey newKey = new RuleKey(processor.getId(), processor.getVersion());
            RuleContext<IN, OUT> rc = createRuleContext(processor, newKey);
            ruleContexts.put(newKey, rc);
            System.out.println("[CEP] Rule update: id=" + newKey.id
                    + (oldKey != null ? ", v" + oldKey.version + " → v" : "")
                    + newKey.version);
        }

        // ③ Handle added (new rule)
        for (PatternProcessor<IN> processor : change.getAdded()) {
            RuleKey newKey = new RuleKey(processor.getId(), processor.getVersion());
            RuleContext<IN, OUT> rc = createRuleContext(processor, newKey);
            ruleContexts.put(newKey, rc);
            System.out.println("[CEP] New rule online: " + newKey);
        }
    }

    /**
     * ★ Handle full rule update (used when TM restarts for recovery).
     */
    @SuppressWarnings("unchecked")
    private void handleFullUpdate(
            UpdatePatternProcessorEvent<IN> event) throws Exception {
        List<PatternProcessor<IN>> processors = event.getProcessors();

        if (processors == null || processors.isEmpty()) {
            System.out.println("[CEP] Received empty rule list, clearing all NFAs");
            for (Map.Entry<RuleKey, RuleContext<IN, OUT>> entry :
                    ruleContexts.entrySet()) {
                pendingStateClear.add(entry.getKey());
                closeRuleContext(entry.getValue());
            }
            ruleContexts.clear();
            return;
        }

        handleUpdatePatternProcessorEvent(event);
    }

    /**
     * ★★★ Multi-rule hot swap core method.
     *
     * <p>Strategy: full replacement. After receiving the new rule list:
     * <ol>
     *   <li>Close NFAs for rules no longer in the new list</li>
     *   <li>Create NFA + independent state for new rules</li>
     *   <li>Mark old rule states for cleanup (deferred until processElement has key context)</li>
     *   <li>Atomically replace the ruleContexts Map</li>
     * </ol>
     */
    @SuppressWarnings("unchecked")
    private void handleUpdatePatternProcessorEvent(
            UpdatePatternProcessorEvent<IN> event) throws Exception {

        List<PatternProcessor<IN>> processors = event.getProcessors();

        // Case A: empty rule list → clear all rules
        if (processors == null || processors.isEmpty()) {
            System.out.println("[CEP] Received empty rule list, clearing all NFAs");
            for (RuleContext<IN, OUT> rc : ruleContexts.values()) {
                pendingStateClear.add(new RuleKey(
                        rc.processor.getId(), rc.processor.getVersion()));
                closeRuleContext(rc);
            }
            ruleContexts.clear();
            return;
        }

        // Case B: build new ruleContexts Map
        Map<RuleKey, RuleContext<IN, OUT>> newContexts =
                new HashMap<>(processors.size());

        for (PatternProcessor<IN> processor : processors) {
            RuleKey ruleKey = new RuleKey(
                    processor.getId(), processor.getVersion());

            // ① Check if an old Context with the same id+version already exists (reusable)
            RuleContext<IN, OUT> existing = ruleContexts.get(ruleKey);

            if (existing != null) {
                // Same id+version: reuse (no need to rebuild)
                newContexts.put(ruleKey, existing);
                System.out.println("[CEP] Rule kept: id=" + ruleKey.id
                        + ", version=" + ruleKey.version + " (unchanged)");
                continue;
            }

            // ② Check for old versions to clean up
            RuleKey oldKey = findExistingRuleById(processor.getId());
            if (oldKey != null) {
                RuleContext<IN, OUT> oldRc = ruleContexts.get(oldKey);
                pendingStateClear.add(oldKey);
                closeRuleContext(oldRc);
                System.out.println("[CEP] Rule version update: id=" + ruleKey.id
                        + ", v" + oldKey.version + " → v" + ruleKey.version);
            } else {
                System.out.println("[CEP] New rule online: id=" + ruleKey.id
                        + ", version=" + ruleKey.version);
            }

            // ③ Create new RuleContext (initialize NFA + independent state)
            RuleContext<IN, OUT> rc = createRuleContext(processor, ruleKey);
            newContexts.put(ruleKey, rc);
        }

        // ④ Clean up rules not in the new list
        for (Map.Entry<RuleKey, RuleContext<IN, OUT>> entry :
                ruleContexts.entrySet()) {
            if (!newContexts.containsKey(entry.getKey())) {
                System.out.println("[CEP] Rule offline: id=" + entry.getKey().id
                        + ", version=" + entry.getKey().version);
                pendingStateClear.add(entry.getKey());
                closeRuleContext(entry.getValue());
            }
        }

        // ⑤ ★ Atomic replacement
        this.ruleContexts = newContexts;

        System.out.println("[CEP] ★ Rules loaded, currently active: "
                + ruleContexts.size() + "");
        for (RuleKey key : ruleContexts.keySet()) {
            System.out.println("[CEP]   - [" + key.id + "] v" + key.version);
        }
    }

    /**
     * ★ Create a complete runtime context for a single rule:
     *   PatternProcessFunction + JSON→Pattern→NFA + independent ValueState(NFAState)
     *   + independent MapState(eventQueue) + independent InternalTimerService + independent Metrics
     */
    @SuppressWarnings("unchecked")
    private RuleContext<IN, OUT> createRuleContext(
            PatternProcessor<IN> processor, RuleKey ruleKey) throws Exception {

        String stateSuffix = ruleKey.id + "-" + ruleKey.version;

        // 1) Initialize PatternProcessFunction
        PatternProcessFunction<IN, OUT> function =
                (PatternProcessFunction<IN, OUT>)
                        processor.getPatternProcessFunction();
        if (function != null) {
            FunctionUtils.setFunctionRuntimeContext(
                    function, getRuntimeContext());
            FunctionUtils.openFunction(function, new Configuration());
        }

        // 2) ★ JSON → Pattern → NFAFactory → NFA
        NFACompiler.NFAFactory<IN> nfaFactory =
                NFACompiler.compileFactory(
                        processor.getPattern(getUserCodeClassloader()),
                        function instanceof TimedOutPartialMatchHandler);
        NFA<IN> nfa = nfaFactory.createNFA();
        nfa.open(getRuntimeContext(), new Configuration());

        // 3) ★ Independent ValueState(NFAState) — state name with suffix
        ValueState<NFAState> computationStates =
                initContext.getKeyedStateStore()
                        .getState(new ValueStateDescriptor<>(
                                NFA_STATE_PREFIX + "-" + stateSuffix,
                                new NFAStateSerializer()));

        // 4) ★ Independent MapState(eventQueue) — state name with suffix
        MapState<Long, List<IN>> elementQueueState =
                initContext.getKeyedStateStore()
                        .getMapState(new MapStateDescriptor<>(
                                EVENT_QUEUE_PREFIX + "-" + stateSuffix,
                                LongSerializer.INSTANCE,
                                new ListSerializer<>(inputSerializer)));

        // 5) ★ Independent InternalTimerService — name with suffix
        InternalTimerService<VoidNamespace> timerService =
                getInternalTimerService(
                        TIMER_SERVICE_PREFIX + "-" + stateSuffix,
                        VoidNamespaceSerializer.INSTANCE, this);

        // 6) CEP TimerService wrapper (used internally by NFA)
        TimerService cepTimerService =
                new TimerServiceImpl(timerService);

        // 7) ★ Independent Metrics
        Counter lateCounter = metrics.counter(
                "numLateRecordsDropped-" + stateSuffix);
        Counter matchCounter = metrics.counter(
                "patternMatchedTimes-" + stateSuffix);

        return new RuleContext<>(processor, nfa, function,
                computationStates, elementQueueState,
                timerService, cepTimerService,
                lateCounter, matchCounter);
    }

    private RuleKey findExistingRuleById(String id) {
        for (RuleKey key : ruleContexts.keySet()) {
            if (key.id.equals(id)) {
                return key;
            }
        }
        return null;
    }

    private void closeRuleContext(RuleContext<IN, OUT> rc) {
        try {
            if (rc.nfa != null) {
                rc.nfa.close();
            }
        } catch (Exception e) {
            LOG.warn("Error closing NFA", e);
        }
        try {
            if (rc.userFunction != null) {
                FunctionUtils.closeFunction(rc.userFunction);
            }
        } catch (Exception e) {
            LOG.warn("Error closing user function", e);
        }
    }

    // ======================== Phase 5: Rule Lifecycle Management ========================

    /**
     * ★ Handle rule lifecycle events (PAUSE / RESUME / DRAIN / DELETE).
     */
    private void handleLifecycleEvent(RuleLifecycleEvent event) {
        String ruleId = event.getRuleId();

        switch (event.getAction()) {
            case PAUSE:
                pausedRuleIds.add(ruleId);
                System.out.println("[CEP] Rule paused: " + ruleId);
                break;

            case RESUME:
                pausedRuleIds.remove(ruleId);
                drainingRuleIds.remove(ruleId);
                System.out.println("[CEP] Rule resumed: " + ruleId);
                break;

            case DRAIN:
                drainingRuleIds.add(ruleId);
                System.out.println("[CEP] Rule draining: " + ruleId);
                break;

            case DELETE:
                pausedRuleIds.remove(ruleId);
                drainingRuleIds.remove(ruleId);
                java.util.List<RuleKey> toRemove = new java.util.ArrayList<>();
                for (Map.Entry<RuleKey, RuleContext<IN, OUT>> entry :
                        ruleContexts.entrySet()) {
                    if (entry.getKey().id.equals(ruleId)) {
                        pendingStateClear.add(entry.getKey());
                        closeRuleContext(entry.getValue());
                        toRemove.add(entry.getKey());
                    }
                }
                for (RuleKey key : toRemove) {
                    ruleContexts.remove(key);
                }
                System.out.println("[CEP] Rule cleaned up: " + ruleId
                        + " (" + toRemove.size() + " version(s))");
                break;
        }
    }

    /** ★ Check whether a rule should skip event processing (paused or draining) */
    private boolean shouldSkipRule(String ruleId) {
        return pausedRuleIds.contains(ruleId) || drainingRuleIds.contains(ruleId);
    }

    // ======================== Inner Classes ========================

    /**
     * ★ Rule runtime context: encapsulates all state and components of a single rule.
     */
    private static class RuleContext<IN, OUT> {
        final PatternProcessor<IN> processor;
        final NFA<IN> nfa;
        final PatternProcessFunction<IN, OUT> userFunction;
        final ValueState<NFAState> computationStates;
        final MapState<Long, List<IN>> elementQueueState;
        final InternalTimerService<VoidNamespace> timerService;
        final TimerService cepTimerService;
        final Counter numLateRecordsDropped;
        final Counter patternMatchedTimes;

        RuleContext(
                PatternProcessor<IN> processor,
                NFA<IN> nfa,
                PatternProcessFunction<IN, OUT> userFunction,
                ValueState<NFAState> computationStates,
                MapState<Long, List<IN>> elementQueueState,
                InternalTimerService<VoidNamespace> timerService,
                TimerService cepTimerService,
                Counter numLateRecordsDropped,
                Counter patternMatchedTimes) {
            this.processor = processor;
            this.nfa = nfa;
            this.userFunction = userFunction;
            this.computationStates = computationStates;
            this.elementQueueState = elementQueueState;
            this.timerService = timerService;
            this.cepTimerService = cepTimerService;
            this.numLateRecordsDropped = numLateRecordsDropped;
            this.patternMatchedTimes = patternMatchedTimes;
        }
    }

    /**
     * ★ Rule unique identifier (id + version).
     */
    static class RuleKey {
        final String id;
        final int version;

        RuleKey(String id, int version) {
            this.id = id;
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof RuleKey)) return false;
            RuleKey r = (RuleKey) o;
            return version == r.version && id.equals(r.id);
        }

        @Override
        public int hashCode() {
            return 31 * id.hashCode() + version;
        }

        @Override
        public String toString() {
            return id + "-v" + version;
        }
    }

    /**
     * OperatorEvent: encapsulates rule list for JM → TM transmission.
     */
    public static class UpdatePatternProcessorEvent<IN>
            implements OperatorEvent {
        private static final long serialVersionUID = 1L;
        private final List<PatternProcessor<IN>> processors;

        public UpdatePatternProcessorEvent(
                List<PatternProcessor<IN>> processors) {
            this.processors = new ArrayList<>(processors);
        }

        public List<PatternProcessor<IN>> getProcessors() {
            return processors;
        }
    }

    /**
     * PatternProcessFunction.Context implementation.
     */
    private class ContextFunctionImpl
            implements PatternProcessFunction.Context {
        private Long timestamp;

        @Override
        @SuppressWarnings("unchecked")
        public <X> void output(OutputTag<X> outputTag, X value) {
            StreamRecord<X> record = isProcessingTime
                    ? new StreamRecord<>(value)
                    : new StreamRecord<>(value, timestamp());
            output.collect(outputTag, record);
        }

        void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public long timestamp() {
            return timestamp;
        }

        @Override
        public long currentProcessingTime() {
            // Use the timerService of the first active rule (time is the same for all rules)
            return ruleContexts.isEmpty() ? 0L
                    : ruleContexts.values().iterator().next()
                            .timerService.currentProcessingTime();
        }
    }

    /**
     * TimerService implementation, provides clock access to NFA.
     */
    private static class TimerServiceImpl implements TimerService {
        private final InternalTimerService<VoidNamespace> timerService;

        TimerServiceImpl(InternalTimerService<VoidNamespace> timerService) {
            this.timerService = timerService;
        }

        @Override
        public long currentProcessingTime() {
            return timerService.currentProcessingTime();
        }
    }
}
