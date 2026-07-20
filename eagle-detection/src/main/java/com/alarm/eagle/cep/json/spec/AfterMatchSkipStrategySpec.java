package com.alarm.eagle.cep.json.spec;

import org.apache.flink.cep.nfa.aftermatch.AfterMatchSkipStrategy;
import org.apache.flink.cep.nfa.aftermatch.SkipToFirstStrategy;
import org.apache.flink.cep.nfa.aftermatch.SkipToLastStrategy;

/**
 * JSON representation of AfterMatchSkipStrategy.
 *
 * <p>JSON Format: {@code {"type":"SKIP_PAST_LAST_EVENT","patternName":null}}
 */
public class AfterMatchSkipStrategySpec {

    private final String type;
    private final String patternName;

    public AfterMatchSkipStrategySpec() {
        this("NO_SKIP", null);
    }

    public AfterMatchSkipStrategySpec(String type, String patternName) {
        this.type = type;
        this.patternName = patternName;
    }

    public String getType() {
        return type;
    }

    public String getPatternName() {
        return patternName;
    }

    /**
     * ★ Restore to Flink AfterMatchSkipStrategy
     */
    public AfterMatchSkipStrategy toAfterMatchSkipStrategy() {
        if (type == null) {
            return AfterMatchSkipStrategy.noSkip();
        }
        switch (type) {
            case "SKIP_TO_NEXT":
                return AfterMatchSkipStrategy.skipToNext();
            case "SKIP_PAST_LAST_EVENT":
                return AfterMatchSkipStrategy.skipPastLastEvent();
            case "SKIP_TO_FIRST":
                return AfterMatchSkipStrategy.skipToFirst(patternName);
            case "SKIP_TO_LAST":
                return AfterMatchSkipStrategy.skipToLast(patternName);
            case "NO_SKIP":
            default:
                return AfterMatchSkipStrategy.noSkip();
        }
    }

    /**
     * Reverse-engineer Spec from Flink AfterMatchSkipStrategy.
     * Determine strategy type by checking class name.
     */
    public static AfterMatchSkipStrategySpec fromAfterMatchSkipStrategy(
            AfterMatchSkipStrategy strategy) {
        if (strategy == null) {
            return new AfterMatchSkipStrategySpec("NO_SKIP", null);
        }

        String className = strategy.getClass().getSimpleName();
        if (className.contains("NoSkip")) {
            return new AfterMatchSkipStrategySpec("NO_SKIP", null);
        } else if (className.contains("SkipToNext")) {
            return new AfterMatchSkipStrategySpec("SKIP_TO_NEXT", null);
        } else if (className.contains("SkipPastLast")) {
            return new AfterMatchSkipStrategySpec("SKIP_PAST_LAST_EVENT", null);
        } else if (strategy instanceof SkipToFirstStrategy) {
            return new AfterMatchSkipStrategySpec("SKIP_TO_FIRST",
                    strategy.getPatternName().orElse(null));
        } else if (strategy instanceof SkipToLastStrategy) {
            return new AfterMatchSkipStrategySpec("SKIP_TO_LAST",
                    strategy.getPatternName().orElse(null));
        }
        return new AfterMatchSkipStrategySpec("NO_SKIP", null);
    }

    @Override
    public String toString() {
        return "AfterMatchSkipStrategySpec{type='" + type
                + "', patternName='" + patternName + "'}";
    }
}
