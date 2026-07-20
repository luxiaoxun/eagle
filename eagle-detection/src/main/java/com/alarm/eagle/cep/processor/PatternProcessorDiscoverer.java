package com.alarm.eagle.cep.processor;

import java.io.Closeable;

/**
 * Rule discoverer interface — responsible for detecting and fetching latest rules from external data sources.
 *
 * @param <T> event type
 */
public interface PatternProcessorDiscoverer<T> extends Closeable {

    /**
     * Start rule discovery and register a callback manager.
     *
     * @param manager callback manager that receives rule updates
     */
    void discoverPatternProcessorUpdates(PatternProcessorManager<T> manager);
}
