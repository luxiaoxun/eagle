package com.alarm.eagle.cep.json.spec;

import org.apache.flink.cep.pattern.conditions.IterativeCondition;

/**
 * Class type condition. Stored as a fully qualified Java class name.
 *
 * <p>JSON Format: {@code {"type":"CLASS","className":"com.alarm.eagle.cep.condition.EndCondition"}}
 */
public class ClassConditionSpec extends ConditionSpec {

    private final String className;

    public ClassConditionSpec() {
        super(ConditionType.CLASS);
        this.className = null;
    }

    public ClassConditionSpec(String className) {
        super(ConditionType.CLASS);
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IterativeCondition<?> toIterativeCondition(
            ClassLoader classLoader) throws Exception {
        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException(
                    "className must not be null or empty");
        }
        Class<?> clazz = classLoader.loadClass(className);
        return (IterativeCondition<?>) clazz.getDeclaredConstructor()
                .newInstance();
    }

    @Override
    public String toString() {
        return "ClassConditionSpec{className='" + className + "'}";
    }
}
