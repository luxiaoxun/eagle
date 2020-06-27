package com.alarm.eagle.config;

/**
 * Created by luxiaoxun on 2020/01/28.
 */
public interface ConfigConstant {

    String FLINK_MODE = "flink.mode";
    String MODE_DEV = "dev";

    String FLINK_PARALLELISM = "flink.parallelism";
    String FLINK_ENABLE_CHECKPOINT = "flink.enable.checkpoint";

    String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";
    String KAFKA_GROUP_ID = "kafka.group.id";
    String KAFKA_TOPIC = "kafka.topic";
    String KAFKA_TOPIC_PARALLELISM = "kafka.topic.parallelism";

    String ELASTICSEARCH_HOSTS = "elasticsearch.hosts";
    String ELASTICSEARCH_BULK_FLUSH_MAX_ACTIONS = "elasticsearch.bulk.flush.max.actions";
    String ELASTICSEARCH_BULK_FLUSH_MAX_SIZE_MB = "elasticsearch.bulk.flush.max.size.mb";
    String ELASTICSEARCH_BULK_FLUSH_INTERVAL_MS = "elasticsearch.bulk.flush.interval.ms";

    String ELASTICSEARCH_SINK_PARALLELISM = "elasticsearch.sink.parallelism";
    String ELASTICSEARCH_INDEX_POSTFIX = "elasticsearch.index.postfix";

    String STREAM_RULE_URL = "stream.rule.url";
    String STREAM_PROCESS_PARALLELISM = "stream.process.parallelism";

    String REDIS_WINDOW_TIME_SECONDS = "redis.window.time.seconds";
    String REDIS_WINDOW_TRIGGER_COUNT = "redis.window.trigger.count";
    String REDIS_CLUSTER_HOSTS = "redis.cluster.hosts";
    String REDIS_SINK_PARALLELISM = "redis.sink.parallelism";
}
