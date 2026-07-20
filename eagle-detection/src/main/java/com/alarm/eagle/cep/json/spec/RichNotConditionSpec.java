package com.alarm.eagle.cep.json.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.RichNotCondition;

/**
 * NOT compound condition Spec.
 *
 * <p>Corresponds to Flink {@link RichNotCondition}: Negate a sub-condition.
 */
public class RichNotConditionSpec extends ConditionSpec {

    private final ConditionSpec condition;

    @JsonCreator
    public RichNotConditionSpec(
            @JsonProperty("condition") ConditionSpec condition) {
        super(ConditionType.NOT);
        this.condition = condition;
    }

    public ConditionSpec getCondition() {
        return condition;
    }

    @Override
    public IterativeCondition<?> toIterativeCondition(ClassLoader classLoader)
            throws Exception {
        if (condition == null) {
            throw new IllegalArgumentException(
                    "NOT condition requires a sub-condition");
        }
        return new RichNotCondition<>(
                condition.toIterativeCondition(classLoader));
    }
}
