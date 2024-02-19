package com.alarm.eagle.log;

import com.alarm.eagle.util.JsonUtil;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Created by luxiaoxun on 2020/01/27.
 */
public class LogSchema implements DeserializationSchema<LogEvent>, SerializationSchema<LogEvent>, KafkaRecordDeserializationSchema<LogEvent> {
    private static final Logger logger = LoggerFactory.getLogger(LogSchema.class);

    private String logIndex = null;

    public LogSchema() {
    }

    public LogSchema(String logIndex) {
        this.logIndex = logIndex;
    }

    @Override
    public void open(DeserializationSchema.InitializationContext context) throws Exception {
        DeserializationSchema.super.open(context);
    }

    @Override
    public LogEvent deserialize(byte[] bytes) {
        String msg = new String(bytes);
        try {
            Map<String, Object> data = JsonUtil.jsonToObjectHashMap(msg, String.class, Object.class);
            LogEvent logEvent = new LogEvent(data);
            if (logIndex != null) {
                logEvent.setIndex(logIndex);
            }
            return logEvent;
        } catch (Exception e) {
            logger.error("Cannot parse log:{}, exception:{}", msg, e);
        }
        return null;
    }

    @Override
    public byte[] serialize(LogEvent logEvent) {
        return JsonUtil.encode(logEvent).getBytes();
    }

    @Override
    public boolean isEndOfStream(LogEvent logEvent) {
        return false;
    }

    @Override
    public TypeInformation<LogEvent> getProducedType() {
        return TypeExtractor.getForClass(LogEvent.class);
    }

    @Override
    public void deserialize(ConsumerRecord<byte[], byte[]> consumerRecord, Collector<LogEvent> collector) throws IOException {
        LogEvent logEvent = deserialize(consumerRecord.value());
        if (logEvent != null) {
            logger.debug("Received log:{}", logEvent);
            collector.collect(logEvent);
        }
    }
}
