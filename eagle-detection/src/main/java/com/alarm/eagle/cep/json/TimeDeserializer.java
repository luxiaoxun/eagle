package com.alarm.eagle.cep.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Jackson deserializer for Flink {@link Time}.
 *
 * <p>JSON format: {@code {"unit":"MINUTES","size":15}} or {@code null}.
 */
public class TimeDeserializer extends JsonDeserializer<Time> {

    @Override
    public Time deserialize(JsonParser p, DeserializationContext ctx)
            throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }

        String unit = null;
        long size = 0;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();
            if ("unit".equals(fieldName)) {
                unit = p.getValueAsString();
            } else if ("size".equals(fieldName)) {
                size = p.getValueAsLong();
            }
        }

        if (unit == null) {
            return null;
        }
        return Time.of(size, TimeUnit.valueOf(unit));
    }
}
