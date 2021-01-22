package com.alarm.eagle.sink;

import java.io.IOException;

import com.alarm.eagle.config.AlarmConfigConstant;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

public class LatencySink {

    public static SinkFunction<String> createLatencySink(ParameterTool parameter) throws IOException {

        String latencySink = parameter.get(AlarmConfigConstant.LATENCY_SINK_TYPE);
        Type latencySinkType = Type.valueOf(latencySink.toUpperCase());

        switch (latencySinkType) {
            case KAFKA:
                String kafkaBootstrapServers = parameter.get(AlarmConfigConstant.KAFKA_LATENCY_BOOTSTRAP_SERVERS);
                String latencyTopic = parameter.get(AlarmConfigConstant.KAFKA_LATENCY_TOPIC);
                return new FlinkKafkaProducer<>(kafkaBootstrapServers, latencyTopic, new SimpleStringSchema());
            case STDOUT:
                return new PrintSinkFunction<>(true);
            default:
                throw new IllegalArgumentException(
                        "Source \"" + latencySinkType + "\" unknown. Known values are:" + Type.values());
        }
    }

    public enum Type {
        KAFKA("Latency Sink (Kafka)"),
        PUBSUB("Latency Sink (Pub/Sub)"),
        STDOUT("Latency Sink (Std. Out)");

        private String name;

        Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
