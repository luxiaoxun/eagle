package com.alarm.eagle.cep.json.spec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.cep.pattern.WithinType;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.util.concurrent.TimeUnit;

/**
 * Time window definition.
 *
 * <p>Corresponds to {@code pattern.within(Time.minutes(10))} in Flink CEP.
 * JSON Format: {@code {"type":"FIRST_AND_LAST","size":1,"unit":"HOURS"}}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WindowSpec {

    private final String type;   // "FIRST_AND_LAST" / "PREVIOUS_AND_CURRENT"
    private final int size;
    private final String unit;   // "DAYS"/"HOURS"/"MINUTES"/"SECONDS"/"MILLISECONDS"

    public WindowSpec() {
        this(null, 0, null);
    }

    public WindowSpec(String type, int size, String unit) {
        this.type = type;
        this.size = size;
        this.unit = unit;
    }

    public WithinType getType() {
        if (type == null) {
            return WithinType.FIRST_AND_LAST;
        }
        return WithinType.valueOf(type);
    }

    /**
     * ★ Computed property, not a field. Marked @JsonIgnore to avoid Jackson serialization/deserialization.
     */
    @JsonIgnore
    public Time getTime() {
        return Time.of(size, TimeUnit.valueOf(unit));
    }

    @JsonIgnore
    public String getTypeStr() {
        return type;
    }

    public int getSize() {
        return size;
    }

    public String getUnit() {
        return unit;
    }

    /**
     * Construct from Flink WindowTime Map
     */
    public static WindowSpec fromWindowTime(
            WithinType withinType, Time time) {
        String unitStr = null;
        int s = 0;
        if (time != null) {
            s = (int) time.getSize();
            unitStr = time.getUnit().name();
        }
        return new WindowSpec(withinType.name(), s, unitStr);
    }

    @Override
    public String toString() {
        return "WindowSpec{type=" + type + ", size=" + size + ", unit=" + unit + '}';
    }
}
