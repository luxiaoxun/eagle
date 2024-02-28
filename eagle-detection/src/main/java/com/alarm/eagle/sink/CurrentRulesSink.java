package com.alarm.eagle.sink;

import com.alarm.eagle.config.Constant;
import com.alarm.eagle.functions.JsonSerializer;
import com.alarm.eagle.rule.Rule;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

public class CurrentRulesSink {

    public static SinkFunction<String> createRulesSink(ParameterTool parameter) {

        String sinkType = parameter.get(Constant.RULE_EXPORT_TYPE);
        Type currentRulesSinkType = Type.valueOf(sinkType.toUpperCase());
        switch (currentRulesSinkType) {
            case KAFKA:
                String kafkaBootstrapServers = parameter.get(Constant.KAFKA_RULE_EXPORT_BOOTSTRAP_SERVERS);
                String ruleExportTopic = parameter.get(Constant.KAFKA_RULE_EXPORT_TOPIC);
                return new FlinkKafkaProducer<>(kafkaBootstrapServers, ruleExportTopic, new SimpleStringSchema());
            case STDOUT:
                return new PrintSinkFunction<>(true);
            default:
                throw new IllegalArgumentException(
                        "Source \"" + currentRulesSinkType + "\" unknown. Known values are:" + Type.values());
        }
    }

    public static DataStream<String> rulesStreamToJson(DataStream<Rule> alerts) {
        return alerts.flatMap(new JsonSerializer<>(Rule.class)).name("Rules Deserialization");
    }

    public enum Type {
        KAFKA("Current Rules Sink (Kafka)"),
        STDOUT("Current Rules Sink (Std. Out)");

        private String name;

        Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
