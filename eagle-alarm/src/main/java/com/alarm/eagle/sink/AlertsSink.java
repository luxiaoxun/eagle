package com.alarm.eagle.sink;

import com.alarm.eagle.config.AlarmConfigConstant;
import com.alarm.eagle.functions.JsonSerializer;
import com.alarm.eagle.message.Alert;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

public class AlertsSink {

    public static SinkFunction<String> createAlertsSink(ParameterTool parameter) {
        String sinkType = parameter.get(AlarmConfigConstant.ALERT_SINK_TYPE);
        Type alertsSinkType = Type.valueOf(sinkType.toUpperCase());
        switch (alertsSinkType) {
            case KAFKA:
                String kafkaBootstrapServers = parameter.get(AlarmConfigConstant.KAFKA_ALERT_BOOTSTRAP_SERVERS);
                String alertTopic = parameter.get(AlarmConfigConstant.KAFKA_ALERT_TOPIC);
                return new FlinkKafkaProducer<>(kafkaBootstrapServers, alertTopic, new SimpleStringSchema());
            case STDOUT:
                return new PrintSinkFunction<>(true);
            default:
                throw new IllegalArgumentException(
                        "Source \"" + alertsSinkType + "\" unknown. Known values are:" + Type.values());
        }
    }

    public static DataStream<String> alertsStreamToJson(DataStream<Alert> alerts) {
        return alerts.flatMap(new JsonSerializer<>(Alert.class)).name("Alerts Deserialization");
    }

    public enum Type {
        KAFKA("Alerts Sink (Kafka)"),
        STDOUT("Alerts Sink (Std. Out)");

        private String name;

        Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
