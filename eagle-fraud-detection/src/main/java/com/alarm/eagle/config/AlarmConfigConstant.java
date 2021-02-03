package com.alarm.eagle.config;

public interface AlarmConfigConstant {

    String FLINK_MODE = "mode";
    String MODE_DEV = "dev";
    String MODE_TEST = "test";
    String MODE_PROD = "prod";

    String FLINK_PARALLELISM = "flink.parallelism";
    String FLINK_ENABLE_CHECKPOINT = "flink.enable.checkpoint";

    String STREAM_RULE_URL = "stream.rule.url";

    String TRANSACTIONS_SOURCE_TYPE = "transaction.source.type";
    String TRANSACTIONS_PER_SECOND = "transaction.per.second";
    String TRANSACTIONS_OUT_OF_ORDERNESS = "transaction.out.of.orderness";

    String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";
    String KAFKA_GROUP_ID = "kafka.group.id";
    String KAFKA_TOPIC = "kafka.topic";
    String KAFKA_TOPIC_PARALLELISM = "kafka.topic.parallelism";

    String ALERT_SINK_TYPE = "alert.sink.type";
    String KAFKA_ALERT_BOOTSTRAP_SERVERS = "kafka.alert.bootstrap.servers";
    String KAFKA_ALERT_TOPIC = "kafka.alert.topic";

    String RULE_EXPORT_TYPE = "rule.export.type";
    String KAFKA_RULE_EXPORT_BOOTSTRAP_SERVERS = "kafka.rule.export.bootstrap.servers";
    String KAFKA_RULE_EXPORT_TOPIC = "kafka.rule.export.topic";

    String LATENCY_SINK_TYPE = "latency.sink.type";
    String KAFKA_LATENCY_BOOTSTRAP_SERVERS = "kafka.latency.bootstrap.servers";
    String KAFKA_LATENCY_TOPIC = "kafka.latency.topic";

}
