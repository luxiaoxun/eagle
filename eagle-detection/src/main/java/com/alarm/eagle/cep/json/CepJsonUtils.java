package com.alarm.eagle.cep.json;

import com.alarm.eagle.cep.json.spec.GraphSpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON ↔ Pattern bidirectional conversion utility.
 *
 * <p>Core pipeline:
 * <pre>
 *   Pattern —(GraphSpec.fromPattern)→ GraphSpec —(ObjectMapper)→ JSON String
 *   JSON String —(ObjectMapper)→ GraphSpec —(toPattern)→ Pattern
 * </pre>
 */
public class CepJsonUtils {

    private static final Logger LOG =
            LoggerFactory.getLogger(CepJsonUtils.class);

    /** ★ Configure Jackson ObjectMapper (register polymorphic types + Time deserializer) */
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new SimpleModule()
                    .addDeserializer(Time.class, new TimeDeserializer()))
            .enable(SerializationFeature.INDENT_OUTPUT);

    /** Expose for external use (e.g. JDBCPeriodicPatternProcessorDiscoverer) */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Pattern → JSON String (for debugging/development).
     */
    public static String convertPatternToJSONString(
            Pattern<?, ?> pattern) throws JsonProcessingException {
        GraphSpec graphSpec = GraphSpec.fromPattern(pattern);
        return objectMapper.writeValueAsString(graphSpec);
    }

    /**
     * ★ JSON String → Pattern (core method).
     *
     * @param jsonString  GraphSpec JSON string
     * @param classLoader user code ClassLoader
     * @return reconstructed Flink Pattern object
     */
    @SuppressWarnings("unchecked")
    public static <T> Pattern<T, ?> convertJSONStringToPattern(
            String jsonString, ClassLoader classLoader) throws Exception {
        if (classLoader == null) {
            LOG.warn("classLoader is null, using current thread context classLoader.");
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        GraphSpec graphSpec = objectMapper.readValue(jsonString, GraphSpec.class);
        LOG.debug("Deserialized GraphSpec: {}", graphSpec);
        return (Pattern<T, ?>) graphSpec.toPattern(classLoader);
    }

    /**
     * JSON String → GraphSpec (without rendering to Pattern).
     */
    public static GraphSpec convertJSONStringToGraphSpec(
            String jsonString) throws JsonProcessingException {
        return objectMapper.readValue(jsonString, GraphSpec.class);
    }

    /**
     * GraphSpec → JSON String.
     */
    public static String convertGraphSpecToJSONString(
            GraphSpec graphSpec) throws JsonProcessingException {
        return objectMapper.writeValueAsString(graphSpec);
    }
}
