package com.alarm.eagle.config;

import org.apache.flink.api.java.utils.ParameterTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class EagleAlarmProperties {
    private static final Logger logger = LoggerFactory.getLogger(EagleAlarmProperties.class);

    private ParameterTool parameter;
    private static ParameterTool params;
    private static String mode = "";

    private static class PropertiesHolder {
        private static final EagleAlarmProperties holder = new EagleAlarmProperties();
    }

    public static EagleAlarmProperties getInstance(ParameterTool parameterTool) {
        params = parameterTool;
        mode = params.get(AlarmConfigConstant.FLINK_MODE);
        return PropertiesHolder.holder;
    }

    private EagleAlarmProperties() {
        init();
    }

    private void init() {
        try {
            logger.info("load parameters from system ...");
            Map<String, String> map = new HashMap<>();
            switch (mode) {
                case AlarmConfigConstant.MODE_DEV:
                    map.put(AlarmConfigConstant.FLINK_MODE, AlarmConfigConstant.MODE_DEV);
                    map.put(AlarmConfigConstant.STREAM_RULE_URL, "http://localhost:9080/eagle-api/alarm/rules");

                    map.put(AlarmConfigConstant.TRANSACTIONS_SOURCE_TYPE, "GENERATOR");
                    map.put(AlarmConfigConstant.TRANSACTIONS_PER_SECOND, "5");
                    map.put(AlarmConfigConstant.TRANSACTIONS_OUT_OF_ORDERNESS, "60000");

                    map.put(AlarmConfigConstant.KAFKA_BOOTSTRAP_SERVERS, "168.11.101.22:9092");
                    map.put(AlarmConfigConstant.KAFKA_GROUP_ID, "eagle-transaction-" + AlarmConfigConstant.MODE_DEV);
                    map.put(AlarmConfigConstant.KAFKA_TOPIC, "eagle-transaction");
                    map.put(AlarmConfigConstant.KAFKA_TOPIC_PARALLELISM, "3");

                    map.put(AlarmConfigConstant.ALERT_SINK_TYPE, "STDOUT");
                    map.put(AlarmConfigConstant.KAFKA_ALERT_BOOTSTRAP_SERVERS, "168.11.101.22:9092");
                    map.put(AlarmConfigConstant.KAFKA_ALERT_TOPIC, "eagle-transaction-alert");

                    map.put(AlarmConfigConstant.RULE_EXPORT_TYPE, "STDOUT");
                    map.put(AlarmConfigConstant.KAFKA_RULE_EXPORT_BOOTSTRAP_SERVERS, "168.11.101.22:9092");
                    map.put(AlarmConfigConstant.KAFKA_RULE_EXPORT_TOPIC, "eagle-transaction-rule");

                    map.put(AlarmConfigConstant.LATENCY_SINK_TYPE, "STDOUT");
                    map.put(AlarmConfigConstant.KAFKA_LATENCY_BOOTSTRAP_SERVERS, "168.11.101.22:9092");
                    map.put(AlarmConfigConstant.KAFKA_LATENCY_TOPIC, "eagle-transaction-rule");

                    break;
                case AlarmConfigConstant.MODE_TEST:
                    // ....
                    break;
            }
            map.put(AlarmConfigConstant.FLINK_PARALLELISM, "2");
            map.put(AlarmConfigConstant.FLINK_ENABLE_CHECKPOINT, "true");

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
