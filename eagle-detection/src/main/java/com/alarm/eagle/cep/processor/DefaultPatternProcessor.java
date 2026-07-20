package com.alarm.eagle.cep.processor;

import com.alarm.eagle.cep.json.CepJsonUtils;
import org.apache.flink.cep.functions.PatternProcessFunction;
import org.apache.flink.cep.pattern.Pattern;

import java.util.Objects;

/**
 * Default PatternProcessor implementation.
 *
 * <p><b>Design rationale</b>: holds JSON string rather than Pattern object because:
 * <ul>
 *   <li>Pattern gets modified during NFA compilation (e.g. untilCondition injection)</li>
 *   <li>JSON is pure data, naturally serializable, supports JM→TM RPC transport</li>
 *   <li>Each version is independent, no side effects from Pattern object mutation</li>
 * </ul>
 *
 * @param <T> event type
 */
public class DefaultPatternProcessor<T> implements PatternProcessor<T> {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final int version;
    /** ★ Pattern JSON string (GraphSpec format) */
    private final String patternStr;
    /** Match callback, may be null */
    private final PatternProcessFunction<T, ?> function;

    public DefaultPatternProcessor(
            String id,
            int version,
            String patternStr,
            PatternProcessFunction<T, ?> function) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.version = version;
        this.patternStr = Objects.requireNonNull(patternStr,
                "patternStr must not be null");
        this.function = function;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pattern<T, ?> getPattern(ClassLoader classLoader) {
        try {
            return (Pattern<T, ?>) CepJsonUtils
                    .convertJSONStringToPattern(patternStr, classLoader);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to deserialize pattern for rule: " + id, e);
        }
    }

    @Override
    public PatternProcessFunction<T, ?> getPatternProcessFunction() {
        return function;
    }

    @Override
    public String getPatternJSON() {
        return patternStr;
    }

    @Override
    public String toString() {
        return "DefaultPatternProcessor{id='" + id
                + "', version=" + version + '}';
    }
}
