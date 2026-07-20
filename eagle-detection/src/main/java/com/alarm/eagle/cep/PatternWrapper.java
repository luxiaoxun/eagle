package com.alarm.eagle.cep;

import org.apache.flink.cep.nfa.aftermatch.AfterMatchSkipStrategy;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.Quantifier;

/**
 * Wrapper class that exposes Pattern's protected constructor.
 *
 * <p>Flink's {@link Pattern} constructor is {@code protected},
 * callable by subclasses in any package via {@code super()}.
 * This wrapper exposes it as a public constructor for JSON → Pattern deserialization.
 */
public class PatternWrapper<T, F extends T> extends Pattern<T, F> {

    public PatternWrapper(
            String name,
            Pattern<T, ? extends T> previous,
            Quantifier.ConsumingStrategy consumingStrategy,
            AfterMatchSkipStrategy afterMatchSkipStrategy) {
        super(name, previous, consumingStrategy, afterMatchSkipStrategy);
    }
}
