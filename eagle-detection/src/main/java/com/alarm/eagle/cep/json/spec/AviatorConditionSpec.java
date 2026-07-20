package com.alarm.eagle.cep.json.spec;

import com.alarm.eagle.cep.condition.AviatorCondition;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;

/**
 * Aviator expression type condition. Stored as an expression string.
 *
 * <p>JSON Format: {@code {"type":"AVIATOR","expression":"action == 0"}}
 */
public class AviatorConditionSpec extends ConditionSpec {

    private final String expression;

    public AviatorConditionSpec() {
        super(ConditionType.AVIATOR);
        this.expression = null;
    }

    public AviatorConditionSpec(String expression) {
        super(ConditionType.AVIATOR);
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public IterativeCondition<?> toIterativeCondition(
            ClassLoader classLoader) {
        return new AviatorCondition<>(expression);
    }

    @Override
    public String toString() {
        return "AviatorConditionSpec{expression='" + expression + "'}";
    }
}
