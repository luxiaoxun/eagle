package com.alarm.eagle.config;

import org.apache.flink.api.java.utils.ParameterTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DetectionProperties {
    private static final Logger logger = LoggerFactory.getLogger(DetectionProperties.class);

    private ParameterTool parameter;
    private static ParameterTool params;
    private static String mode = "";

    private static class PropertiesHolder {
        private static final DetectionProperties holder = new DetectionProperties();
    }

    public static DetectionProperties getInstance(ParameterTool parameterTool) {
        params = parameterTool;
        mode = params.get(Constant.FLINK_MODE);
        return PropertiesHolder.holder;
    }

    private DetectionProperties() {
        init();
    }

    private void init() {
        try {
            logger.info("load parameters from system ...");
            Map<String, String> map = new HashMap<>();
            switch (mode) {
                case Constant.MODE_DEV:
                    map.put(Constant.FLINK_MODE, Constant.MODE_DEV);
                    map.put(Constant.STREAM_RULE_URL, "http://localhost:9080/eagle-api/alarm/rules");

                    map.put(Constant.TRANSACTIONS_SOURCE_TYPE, "GENERATOR");
                    map.put(Constant.TRANSACTIONS_PER_SECOND, "5");
                    map.put(Constant.TRANSACTIONS_OUT_OF_ORDERNESS, "60000");

                    map.put(Constant.KAFKA_BOOTSTRAP_SERVERS, "168.11.101.22:9092");
                    map.put(Constant.KAFKA_GROUP_ID, "eagle-transaction-" + Constant.MODE_DEV);
                    map.put(Constant.KAFKA_TOPIC, "eagle-transaction");
                    map.put(Constant.KAFKA_TOPIC_PARALLELISM, "3");

                    map.put(Constant.ALERT_SINK_TYPE, "STDOUT");
                    map.put(Constant.KAFKA_ALERT_BOOTSTRAP_SERVERS, "168.11.101.22:9092");
                    map.put(Constant.KAFKA_ALERT_TOPIC, "eagle-transaction-alert");

                    map.put(Constant.RULE_EXPORT_TYPE, "STDOUT");
                    map.put(Constant.KAFKA_RULE_EXPORT_BOOTSTRAP_SERVERS, "168.11.101.22:9092");
                    map.put(Constant.KAFKA_RULE_EXPORT_TOPIC, "eagle-transaction-rule");

                    map.put(Constant.LATENCY_SINK_TYPE, "STDOUT");
                    map.put(Constant.KAFKA_LATENCY_BOOTSTRAP_SERVERS, "168.11.101.22:9092");
                    map.put(Constant.KAFKA_LATENCY_TOPIC, "eagle-transaction-rule");

                    break;
                case Constant.MODE_TEST:
                    // ....
                    break;
            }
            map.put(Constant.FLINK_PARALLELISM, "2");
            map.put(Constant.FLINK_ENABLE_CHECKPOINT, "true");

            Map<String, String> paramsMap = params.toMap();
            map.putAll(paramsMap);
            parameter = ParameterTool.fromMap(map);

        } catch (Exception ex) {
            logger.error("load parameters from system error: " + ex.toString());
        }
    }

    public ParameterTool getParameter() {
        return parameter;
    }

}
