package com.alarm.eagle.log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by luxiaoxun on 2020/01/27.
 */
public class LogSchema implements DeserializationSchema<LogEntry>, SerializationSchema<LogEntry> {
    private static final Logger logger = LoggerFactory.getLogger(LogSchema.class);

    private String logIndex = null;

    public LogSchema() {
    }

    public LogSchema(String logIndex) {
        this.logIndex = logIndex;
    }

    @Override
    public LogEntry deserialize(byte[] bytes) {
        String msg = new String(bytes);
        try {
            LogEntry entry = new LogEntry((JsonObject) JsonParser.parseString(msg));
            if (logIndex != null) {
                entry.setIndex(logIndex);
            }
            return entry;
        } catch (Exception e) {
            logger.error("Cannot parse log:{}, exception:{}", msg, e);
        }
        return null;
    }

    @Override
    public byte[] serialize(LogEntry logEntry) {
        return logEntry.toJSON().toString().getBytes();
    }

    @Override
    public boolean isEndOfStream(LogEntry logEntry) {
        return false;
    }

    @Override
    public TypeInformation<LogEntry> getProducedType() {
        return TypeExtractor.getForClass(LogEntry.class);
    }


}
