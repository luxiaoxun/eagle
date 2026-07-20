package com.alarm.eagle.cep.processor;

import org.apache.flink.cep.functions.PatternProcessFunction;
import org.apache.flink.cep.pattern.Pattern;

import java.io.Serializable;

/**
 * Complete abstraction of a single CEP rule.
 *
 * <p>Each rule is uniquely identified by id + version. Contains:
 * <ul>
 *   <li>Rule identifier (id + version)</li>
 *   <li>Pattern definition (as JSON string, deserialized at runtime)</li>
 *   <li>Callback function on match (PatternProcessFunction)</li>
 * </ul>
 *
 * @param <T> event type
 */
public interface PatternProcessor<T> extends Serializable {

    /** Unique rule identifier */
    String getId();

    /** Rule version number (incrementing) */
    int getVersion();

    /**
     * ★ Get the Pattern object.
     * Deserialized from JSON on every call to prevent internal Pattern state from being shared across versions.
     *
     * @param classLoader user code ClassLoader, for loading Class-type conditions
     */
    Pattern<T, ?> getPattern(ClassLoader classLoader);

    /** Callback function invoked on match */
    PatternProcessFunction<T, ?> getPatternProcessFunction();

    /** Pattern JSON string */
    String getPatternJSON();
}
