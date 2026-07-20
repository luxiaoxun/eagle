package com.alarm.eagle.cep.json.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.alarm.eagle.cep.PatternWrapper;
import org.apache.flink.cep.nfa.aftermatch.AfterMatchSkipStrategy;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.Quantifier;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.RichOrCondition;

/**
 * JSON representation of a single Pattern node.
 *
 * <p>Corresponds to one node of {@code pattern.where(condition).timesOrMore(n)} in Flink CEP.
 * Supports two types: ATOMIC (atomic node) and COMPOSITE (nested sub-Graph).
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = NodeSpec.class, name = "ATOMIC"),
        @JsonSubTypes.Type(value = GraphSpec.class, name = "COMPOSITE")
})
public class NodeSpec {

    public enum PatternNodeType {
        ATOMIC,
        COMPOSITE
    }

    private final String name;
    private final QuantifierSpec quantifier;
    private final ConditionSpec condition;
    private final PatternNodeType type;

    public NodeSpec() {
        this(null, new QuantifierSpec(), null, PatternNodeType.ATOMIC);
    }

    public NodeSpec(String name, QuantifierSpec quantifier,
                    ConditionSpec condition, PatternNodeType type) {
        this.name = name;
        this.quantifier = quantifier != null ? quantifier : new QuantifierSpec();
        this.condition = condition;
        this.type = type != null ? type : PatternNodeType.ATOMIC;
    }

    public String getName() {
        return name;
    }

    public QuantifierSpec getQuantifier() {
        return quantifier;
    }

    public ConditionSpec getCondition() {
        return condition;
    }

    public PatternNodeType getType() {
        return type;
    }

    /**
     * Construct NodeSpec from a Flink Pattern.
     */
    public static NodeSpec fromPattern(Pattern<?, ?> pattern) {
        QuantifierSpec q = new QuantifierSpec(
                pattern.getQuantifier(),
                pattern.getTimes(),
                pattern.getUntilCondition());
        ConditionSpec c = ConditionSpec.fromCondition(pattern.getCondition());
        return new NodeSpec(pattern.getName(), q, c, PatternNodeType.ATOMIC);
    }

    /**
     * ★★ Core method: restore a NodeSpec to a Flink Pattern node.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Pattern<?, ?> toPattern(
            Pattern<?, ?> previous,
            AfterMatchSkipStrategy afterMatchSkipStrategy,
            Quantifier.ConsumingStrategy consumingStrategy,
            ClassLoader classLoader) throws Exception {

        // If it is a GraphSpec (COMPOSITE), delegate to its own toPattern
        if (this instanceof GraphSpec) {
            return ((GraphSpec) this).toPattern(classLoader);
        }

        // ★ Call the protected constructor via PatternWrapper
        Pattern pattern = new PatternWrapper(
                this.getName(), previous,
                consumingStrategy, afterMatchSkipStrategy);

        // ① Set condition (where / or)
        if (condition != null) {
            IterativeCondition iterativeCondition =
                    condition.toIterativeCondition(classLoader);
            if (iterativeCondition instanceof RichOrCondition) {
                pattern.or(iterativeCondition);
            } else {
                pattern.where(iterativeCondition);
            }
        }

        // ② Set quantifier according to quantifier properties
        TimesSpec times = quantifier.getTimes();
        for (Quantifier.QuantifierProperty prop : quantifier.getProperties()) {
            switch (prop) {
                case OPTIONAL:
                    pattern.optional();
                    break;
                case GREEDY:
                    pattern.greedy();
                    break;
                case LOOPING:
                    if (times != null) {
                        pattern.timesOrMore(times.getFrom(), times.getWindowTime());
                    }
                    break;
                case TIMES:
                    if (times != null) {
                        pattern.times(times.getFrom(), times.getTo());
                    }
                    break;
                default:
                    break;
            }
        }

        // ③ Set internal contiguity (only applicable to nodes that allow repetition like LOOPING / TIMES)
        if (!quantifier.getProperties().contains(Quantifier.QuantifierProperty.SINGLE)) {
            Quantifier.ConsumingStrategy inner = quantifier.getConsumingStrategy();
            if (inner == Quantifier.ConsumingStrategy.SKIP_TILL_ANY) {
                pattern.allowCombinations();
            } else if (inner == Quantifier.ConsumingStrategy.STRICT) {
                pattern.consecutive();
            }
        }

        // ④ Set untilCondition
        if (quantifier.getUntilCondition() != null) {
            IterativeCondition untilCond =
                    quantifier.getUntilCondition().toIterativeCondition(classLoader);
            pattern.until(untilCond);
        }

        return pattern;
    }

    @Override
    public String toString() {
        return "NodeSpec{name='" + name + "', type=" + type
                + ", quantifier=" + quantifier + '}';
    }
}
