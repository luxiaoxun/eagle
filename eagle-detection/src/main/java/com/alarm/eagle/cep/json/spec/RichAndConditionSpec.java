package com.alarm.eagle.cep.json.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.RichAndCondition;

import java.util.List;

/**
 * AND compound condition Spec.
 *
 * <p>Corresponds to Flink {@link RichAndCondition}: true if all sub-conditions are true.
 */
public class RichAndConditionSpec extends ConditionSpec {

    private final List<ConditionSpec> conditions;

    @JsonCreator
    public RichAndConditionSpec(
            @JsonProperty("conditions") List<ConditionSpec> conditions) {
        super(ConditionType.AND);
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
                    "AND condition requires at least 2 sub-conditions, got: "
                            + (conditions == null ? 0 : conditions.size()));
        }

        // ★ Recursive expansion
        IterativeCondition<?> result = new RichAndCondition(
                conditions.get(0).toIterativeCondition(classLoader),
                conditions.get(1).toIterativeCondition(classLoader));
        for (int i = 2; i < conditions.size(); i++) {
            result = new RichAndCondition(
                    result,
                    conditions.get(i).toIterativeCondition(classLoader));
        }
        return result;
    }
}
