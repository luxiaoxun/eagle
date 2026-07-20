package com.alarm.eagle.config;

public interface Constant {

    String FLINK_MODE = "mode";
    String MODE_DEV = "dev";
    String MODE_TEST = "test";
    String MODE_PROD = "prod";

    String FLINK_PARALLELISM = "flink.parallelism";
    String FLINK_ENABLE_CHECKPOINT = "flink.enable.checkpoint";

    /** eagle-api 基础 URL（用于拉取 CEP 规则） */
    String API_BASE_URL = "api.base.url";

    /** CEP 规则轮询间隔（毫秒） */
    String CEP_RULE_POLL_INTERVAL_MS = "cep.rule.poll.interval.ms";

    String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";
    String KAFKA_GROUP_ID = "kafka.group.id";
    String KAFKA_TOPIC = "kafka.topic";
    String KAFKA_TOPIC_PARALLELISM = "kafka.topic.parallelism";
    String KAFKA_SASL_USERNAME = "kafka.sasl.username";
    String KAFKA_SASL_PASSWORD = "kafka.sasl.password";

    /** 事件乱序容忍度（毫秒） */
    String TRANSACTIONS_OUT_OF_ORDERNESS = "transaction.out.of.orderness";
}
