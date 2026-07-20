package com.alarm.eagle.cep.json.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.alarm.eagle.cep.condition.AviatorCondition;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.cep.pattern.conditions.RichAndCondition;
import org.apache.flink.cep.pattern.conditions.RichNotCondition;
import org.apache.flink.cep.pattern.conditions.RichOrCondition;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for conditions.
 *
 * <p>Supported subtypes:
 * <ul>
 *   <li>{@code CLASS}   — Java class, loaded via ClassLoader</li>
 *   <li>{@code AVIATOR} — Aviator expression string</li>
 *   <li>{@code OR}      — At least one sub-condition is true (recursive nesting)</li>
 *   <li>{@code AND}     — All sub-conditions are true (recursive nesting)</li>
 *   <li>{@code NOT}     — Negate a sub-condition</li>
 * </ul>
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ClassConditionSpec.class, name = "CLASS"),
        @JsonSubTypes.Type(value = AviatorConditionSpec.class, name = "AVIATOR"),
        @JsonSubTypes.Type(value = RichOrConditionSpec.class, name = "OR"),
        @JsonSubTypes.Type(value = RichAndConditionSpec.class, name = "AND"),
        @JsonSubTypes.Type(value = RichNotConditionSpec.class, name = "NOT"),
})
public abstract class ConditionSpec {

    public enum ConditionType {
        CLASS,
        AVIATOR,
        OR,
        AND,
        NOT
    }

    private final ConditionType type;

    protected ConditionSpec() {
        this.type = ConditionType.CLASS;
    }

    protected ConditionSpec(ConditionType type) {
        this.type = type;
    }

    public ConditionType getType() {
        return type;
    }

    /**
     * ★ Core method: restore ConditionSpec to a Flink IterativeCondition.
     * Supports recursive expansion of compound conditions (OR/AND/NOT).
     */
    public abstract IterativeCondition<?> toIterativeCondition(
            ClassLoader classLoader) throws Exception;

    /**
     * ★ Core method: reverse-engineer ConditionSpec from a Flink IterativeCondition.
     * Supports recursive conversion of compound conditions.
     */
    @SuppressWarnings("unchecked")
    public static ConditionSpec fromCondition(IterativeCondition<?> condition) {
        if (condition == null) {
            return null;
        }

        // OR condition
        if (condition instanceof RichOrCondition) {
            IterativeCondition<?>[] nested =
                    ((RichOrCondition<?>) condition).getNestedConditions();
            List<ConditionSpec> specs = new ArrayList<>(nested.length);
            for (IterativeCondition<?> c : nested) {
                specs.add(fromCondition(c));  // ★ recursive
            }
            return new RichOrConditionSpec(specs);
        }

        // AND condition
        if (condition instanceof RichAndCondition) {
            IterativeCondition<?>[] nested =
                    ((RichAndCondition<?>) condition).getNestedConditions();
            List<ConditionSpec> specs = new ArrayList<>(nested.length);
            for (IterativeCondition<?> c : nested) {
                specs.add(fromCondition(c));  // ★ recursive
            }
            return new RichAndConditionSpec(specs);
        }

        // NOT condition
        if (condition instanceof RichNotCondition) {
            IterativeCondition<?>[] nested =
                    ((RichNotCondition<?>) condition).getNestedConditions();
            return new RichNotConditionSpec(fromCondition(nested[0]));  // ★ recursive
        }

        // AviatorCondition → AVIATOR type
        if (condition instanceof AviatorCondition) {
            return new AviatorConditionSpec(
                    ((AviatorCondition<?>) condition).getExpression());
        }

        // Default → CLASS type (store fully qualified class name)
        return new ClassConditionSpec(condition.getClass().getName());
    }
}
