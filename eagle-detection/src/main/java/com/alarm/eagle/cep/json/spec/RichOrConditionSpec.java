package com.alarm.eagle.cep.json.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.RichOrCondition;

import java.util.List;

/**
 * OR compound condition Spec.
 *
 * <p>Corresponds to Flink {@link RichOrCondition}: true if at least one sub-condition is true.
 */
public class RichOrConditionSpec extends ConditionSpec {

    private final List<ConditionSpec> conditions;

    @JsonCreator
    public RichOrConditionSpec(
            @JsonProperty("conditions") List<ConditionSpec> conditions) {
        super(ConditionType.OR);
        this.conditions = conditions;
    }

    public List<ConditionSpec> getConditions() {
        return conditions;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public IterativeCondition<?> toIterativeCondition(ClassLoader classLoader)
            throws Exception {
        if (conditions == null || conditions.size() < 2) {
            throw new IllegalArgumentException(
                    "OR condition requires at least 2 sub-conditions, got: "
                            + (conditions == null ? 0 : conditions.size()));
        }

        // ★ Recursive expansion: combine first and second conditions with OR, then successively add more
        IterativeCondition<?> result = new RichOrCondition(
                conditions.get(0).toIterativeCondition(classLoader),
                conditions.get(1).toIterativeCondition(classLoader));
        for (int i = 2; i < conditions.size(); i++) {
            result = new RichOrCondition(
                    result,
                    conditions.get(i).toIterativeCondition(classLoader));
        }
        return result;
    }
}
