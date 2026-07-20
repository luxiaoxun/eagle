package com.alarm.eagle.cep.json.spec;

import org.apache.flink.cep.pattern.Quantifier;

/**
 * Edge definition. Describes the connection relationship between two nodes in the Pattern DAG.
 *
 * <p>JSON Format: {@code {"source":"start","target":"end","type":"SKIP_TILL_NEXT"}}
 */
public class EdgeSpec {

    private final String source;
    private final String target;
    private final Quantifier.ConsumingStrategy type;

    public EdgeSpec() {
        this(null, null, Quantifier.ConsumingStrategy.STRICT);
    }

    public EdgeSpec(String source, String target,
                    Quantifier.ConsumingStrategy type) {
        this.source = source;
        this.target = target;
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public Quantifier.ConsumingStrategy getType() {
        return type;
    }

    @Override
    public String toString() {
        return "EdgeSpec{source='" + source + "', target='"
                + target + "', type=" + type + '}';
    }
}
