package com.alarm.eagle.cep.processor;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Abstract periodic polling discoverer.
 *
 * <p>Uses {@link java.util.Timer} for periodic rule checking.
 * Subclasses only need to implement two methods:
 * <ul>
 *   <li>{@link #arePatternProcessorsUpdated()} — check for updates</li>
 *   <li>{@link #getLatestPatternProcessors()} — get the latest rule list</li>
 * </ul>
 *
 * @param <T> event type
 */
public abstract class PeriodicPatternProcessorDiscoverer<T>
        implements PatternProcessorDiscoverer<T> {

    private final Long intervalMillis;
    private final Timer timer;

    /**
     * @param intervalMillis polling interval in milliseconds; first execution is immediate
     */
    protected PeriodicPatternProcessorDiscoverer(Long intervalMillis) {
        this.intervalMillis = intervalMillis;
        this.timer = new Timer();
    }

    // ======================== Subclass Implementation ========================

    /** ★ Check whether rules have been updated */
    public abstract boolean arePatternProcessorsUpdated();

    /** ★ Get the latest rule list */
    public abstract List<PatternProcessor<T>>
        getLatestPatternProcessors() throws Exception;

    // ======================== Scheduling Logic ========================

    @Override
    public void discoverPatternProcessorUpdates(
            PatternProcessorManager<T> manager) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (arePatternProcessorsUpdated()) {
                        List<PatternProcessor<T>> processors =
                                getLatestPatternProcessors();
                        manager.onPatternProcessorsUpdated(processors);
                    }
                } catch (Exception e) {
                    // ★ Critical: uncaught exceptions in TimerTask will kill the entire Timer thread
                    e.printStackTrace();
                }
            }
        }, 0, intervalMillis); // 0ms = execute immediately on first run, then at interval
    }

    @Override
    public void close() throws IOException {
        timer.cancel();
    }
}
