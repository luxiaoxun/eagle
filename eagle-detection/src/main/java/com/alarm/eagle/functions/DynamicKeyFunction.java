package com.alarm.eagle.functions;

import com.alarm.eagle.message.Descriptors;
import com.alarm.eagle.message.Keyed;
import com.alarm.eagle.message.KeysExtractor;
import com.alarm.eagle.message.Transaction;
import com.alarm.eagle.rule.Rule;

import java.util.Iterator;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Gauge;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;

/**
 * Implements dynamic data partitioning based on a set of broadcasted rules.
 */
@Slf4j
public class DynamicKeyFunction
        extends BroadcastProcessFunction<Transaction, Rule, Keyed<Transaction, String, Integer>> {

    private RuleCounterGauge ruleCounterGauge;

    @Override
    public void open(Configuration parameters) {
        ruleCounterGauge = new RuleCounterGauge();
        getRuntimeContext().getMetricGroup().gauge("numberOfActiveRules", ruleCounterGauge);
    }

    @Override
    public void processElement(Transaction event, ReadOnlyContext ctx,
                               Collector<Keyed<Transaction, String, Integer>> out) throws Exception {
        ReadOnlyBroadcastState<Integer, Rule> rulesState = ctx.getBroadcastState(Descriptors.rulesDescriptor);
        int ruleCounter = 0;
        for (Entry<Integer, Rule> entry : rulesState.immutableEntries()) {
            final Rule rule = entry.getValue();
            try {
                String key = KeysExtractor.getKey(rule.getGroupingKeyNames(), event);
                out.collect(new Keyed<>(event, key, rule.getRuleId()));
                ruleCounter++;
            } catch (Exception ex) {
                log.error("Invalid rule: {}", rule);
            }
        }
        ruleCounterGauge.setValue(ruleCounter);
    }

    @Override
    public void processBroadcastElement(Rule rule, Context ctx, Collector<Keyed<Transaction, String, Integer>> out) throws Exception {
        log.info("New rule: {}", rule);
        BroadcastState<Integer, Rule> broadcastState = ctx.getBroadcastState(Descriptors.rulesDescriptor);
        ProcessingUtils.handleRuleBroadcast(rule, broadcastState);
        if (rule.getRuleState() == Rule.RuleState.CONTROL) {
            handleControlCommand(rule.getControlType(), broadcastState);
        }
    }

    private void handleControlCommand(Rule.ControlType controlType, BroadcastState<Integer, Rule> rulesState) throws Exception {
        switch (controlType) {
            case DELETE_RULES_ALL:
                Iterator<Entry<Integer, Rule>> entriesIterator = rulesState.iterator();
                while (entriesIterator.hasNext()) {
                    Entry<Integer, Rule> ruleEntry = entriesIterator.next();
                    rulesState.remove(ruleEntry.getKey());
                    log.info("Removed rule: {}", ruleEntry.getValue());
                }
                break;
        }
    }

    private static class RuleCounterGauge implements Gauge<Integer> {
        private int value = 0;

        public void setValue(int value) {
            this.value = value;
        }

        @Override
        public Integer getValue() {
            return value;
        }
    }
}
