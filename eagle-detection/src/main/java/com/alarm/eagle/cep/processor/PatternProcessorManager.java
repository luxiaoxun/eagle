package com.alarm.eagle.cep.processor;

import java.util.List;

/**
 * Rule manager interface.
 *
 * <p>In Coordinator mode, the JM-side Coordinator implements this interface,
 * receives update callbacks from the Discoverer, and distributes them to all TMs via OperatorEvent.
 *
 * @param <T> event type
 */
public interface PatternProcessorManager<T> {

    /**
     * ★ Rule update callback.
     *
     * @param patternProcessors the latest complete rule list
     */
    void onPatternProcessorsUpdated(
            List<PatternProcessor<T>> patternProcessors);
}
