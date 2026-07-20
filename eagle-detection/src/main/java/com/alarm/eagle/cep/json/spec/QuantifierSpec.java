package com.alarm.eagle.cep.json.spec;

import org.apache.flink.cep.pattern.Quantifier;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;

import java.util.ArrayList;
import java.util.List;

/**
 * Quantifier definition. Corresponds to the Quantifier + Times + untilCondition combination in Flink CEP.
 *
 * <p>Describes "how to match this Pattern node": once, multiple, optional, greedy, and the internal event selection strategy.
 */
public class QuantifierSpec {

    private final Quantifier.ConsumingStrategy consumingStrategy;
    private final List<Quantifier.QuantifierProperty> properties;
    private final TimesSpec times;
    private final ConditionSpec untilCondition;

    public QuantifierSpec() {
        this(Quantifier.ConsumingStrategy.STRICT,
                new ArrayList<>(),
                null, null);
    }

    public QuantifierSpec(Quantifier.ConsumingStrategy consumingStrategy,
                          List<Quantifier.QuantifierProperty> properties,
                          TimesSpec times,
                          ConditionSpec untilCondition) {
        this.consumingStrategy = consumingStrategy;
        this.properties = properties != null ? properties : new ArrayList<>();
        this.times = times;
        this.untilCondition = untilCondition;
    }

    /**
     * Construct from Flink Quantifier + Times + untilCondition.
     */
    public QuantifierSpec(Quantifier quantifier, Quantifier.Times times,
                          IterativeCondition<?> untilCondition) {
        this.consumingStrategy = quantifier.getConsumingStrategy();
        this.properties = new ArrayList<>();
        for (Quantifier.QuantifierProperty prop :
                Quantifier.QuantifierProperty.values()) {
            if (quantifier.hasProperty(prop)) {
                this.properties.add(prop);
            }
        }

        if (times != null) {
            this.times = new TimesSpec(
                    times.getFrom(), times.getTo(), times.getWindowTime());
        } else {
            this.times = null;
        }

        this.untilCondition = untilCondition != null
                ? ConditionSpec.fromCondition(untilCondition) : null;
    }

    public Quantifier.ConsumingStrategy getConsumingStrategy() {
        return consumingStrategy;
    }

    public List<Quantifier.QuantifierProperty> getProperties() {
        return properties;
    }

    public TimesSpec getTimes() {
        return times;
    }

    public ConditionSpec getUntilCondition() {
        return untilCondition;
    }

    @Override
    public String toString() {
        return "QuantifierSpec{consumingStrategy=" + consumingStrategy
                + ", properties=" + properties
                + ", times=" + times
                + ", untilCondition=" + untilCondition + '}';
    }
}
