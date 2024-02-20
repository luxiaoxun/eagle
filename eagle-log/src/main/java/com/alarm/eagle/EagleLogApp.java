package com.alarm.eagle;

import com.alarm.eagle.config.ConfigConstant;
import com.alarm.eagle.config.EagleProperties;
import com.alarm.eagle.rule.RuleUtil;
import com.alarm.eagle.sink.es.ElasticsearchUtil;
import com.alarm.eagle.log.*;
import com.alarm.eagle.rule.RuleBase;
import com.alarm.eagle.sink.redis.LogStatAggregateFunction;
import com.alarm.eagle.sink.redis.LogStatWindowFunction;
import com.alarm.eagle.sink.redis.LogStatWindowResult;
import com.alarm.eagle.sink.redis.RedisAggSinkFunction;
import com.alarm.eagle.source.RuleSourceFunction;
import com.alarm.eagle.util.StringUtil;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.elasticsearch.sink.Elasticsearch7SinkBuilder;
import org.apache.flink.connector.elasticsearch.sink.FlushBackoffType;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.http.HttpHost;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.config.SaslConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by luxiaoxun on 2020/01/27.
 */
public class EagleLogApp {
    private static final Logger logger = LoggerFactory.getLogger(EagleLogApp.class);

    public static void main(String[] args) {
        try {
            ParameterTool params = ParameterTool.fromArgs(args);
            ParameterTool parameter = EagleProperties.getInstance(params).getParameter();
            showConf(parameter);

            // Build stream DAG
            StreamExecutionEnvironment env = getStreamExecutionEnvironment(parameter);
            DataStream<LogEvent> dataSource = getKafkaDataSource(parameter, env);
            BroadcastStream<RuleBase> ruleSource = getRuleDataSource(parameter, env);
            SingleOutputStreamOperator<LogEvent> processedStream = processLogStream(parameter, dataSource, ruleSource);
            sinkToRedis(parameter, processedStream);
            sinkToElasticsearch(parameter, processedStream);

//            DataStream<> kafkaOutputStream = processedStream.getSideOutput(Descriptors.kafkaOutputTag);
//            sinkLogToKafka(parameter, kafkaOutputStream);

            env.getConfig().setGlobalJobParameters(parameter);
            env.execute("eagle-log");
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    private static StreamExecutionEnvironment getStreamExecutionEnvironment(ParameterTool parameter) {
        StreamExecutionEnvironment env = null;
        int globalParallelism = parameter.getInt(ConfigConstant.FLINK_PARALLELISM);
        if (parameter.get(ConfigConstant.FLINK_MODE).equals(ConfigConstant.MODE_DEV)) {
            env = StreamExecutionEnvironment.createLocalEnvironment();
            globalParallelism = 1;
        } else {
            env = StreamExecutionEnvironment.getExecutionEnvironment();
        }
        env.setParallelism(globalParallelism);

        //checkpoint
        boolean enableCheckpoint = parameter.getBoolean(ConfigConstant.FLINK_ENABLE_CHECKPOINT, false);
        if (enableCheckpoint) {
            env.enableCheckpointing(60000L);
            CheckpointConfig config = env.getCheckpointConfig();
            config.setMinPauseBetweenCheckpoints(30000L);
            config.setCheckpointTimeout(10000L);
            //RETAIN_ON_CANCELLATION则在job cancel的时候会保留externalized checkpoint state
            config.setExternalizedCheckpointCleanup(CheckpointConfig.ExternalizedCheckpointCleanup.DELETE_ON_CANCELLATION);
        }

        return env;
    }

    private static BroadcastStream<RuleBase> getRuleDataSource(ParameterTool parameter, StreamExecutionEnvironment env) {
        String ruleUrl = parameter.get(ConfigConstant.STREAM_RULE_URL);
        String ruleName = "rules-source";
        return env.addSource(new RuleSourceFunction(ruleUrl)).name(ruleName).uid(ruleName).setParallelism(1)
                .broadcast(Descriptors.ruleStateDescriptor);
    }

    private static DataStream<LogEvent> getKafkaDataSource(ParameterTool parameter, StreamExecutionEnvironment env) {
        String kafkaBootstrapServers = parameter.get(ConfigConstant.KAFKA_BOOTSTRAP_SERVERS);
        String kafkaGroupId = parameter.get(ConfigConstant.KAFKA_GROUP_ID);
        String kafkaTopic = parameter.get(ConfigConstant.KAFKA_TOPIC);
        int kafkaParallelism = parameter.getInt(ConfigConstant.KAFKA_TOPIC_PARALLELISM);
        String kafkaUsername = parameter.get(ConfigConstant.KAFKA_SASL_USERNAME);
        String kafkaPassword = parameter.get(ConfigConstant.KAFKA_SASL_PASSWORD);
        Properties properties = getProperties(kafkaUsername, kafkaPassword);
        KafkaSource<LogEvent> source = KafkaSource.<LogEvent>builder()
                .setBootstrapServers(kafkaBootstrapServers)
                .setTopics(kafkaTopic)
                .setGroupId(kafkaGroupId)
                .setProperties(properties)
                // Start from committed offset, also use EARLIEST as reset strategy if committed offset doesn't exist
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setDeserializer(new LogSchema())
                .build();

        return env.fromSource(source, WatermarkStrategy.noWatermarks(), kafkaTopic)
                .name(kafkaTopic).uid(kafkaTopic).setParallelism(kafkaParallelism);
    }

    private static SingleOutputStreamOperator<LogEvent> processLogStream(ParameterTool parameter, DataStream<LogEvent> dataSource,
                                                                         BroadcastStream<RuleBase> ruleSource) throws Exception {
        BroadcastConnectedStream<LogEvent, RuleBase> connectedStreams = dataSource.connect(ruleSource);
        int processParallelism = parameter.getInt(ConfigConstant.STREAM_PROCESS_PARALLELISM);
        String kafkaIndex = parameter.get(ConfigConstant.KAFKA_SINK_INDEX);
        String ruleUrl = parameter.get(ConfigConstant.STREAM_RULE_URL);
        RuleBase ruleBase = RuleUtil.getMockRules(ruleUrl);
        if (ruleBase == null) {
            throw new Exception("Can not get initial rules");
        } else {
            String name = "process-log";
            logger.debug("Initial rules: " + ruleBase);
            return connectedStreams.process(new LogProcessFunction(ruleBase, kafkaIndex))
                    .setParallelism(processParallelism).name(name).uid(name);
        }
    }

    private static void sinkLogToKafka(ParameterTool parameter, DataStream<LogEvent> stream) {
        String kafkaBootstrapServers = parameter.get(ConfigConstant.KAFKA_SINK_BOOTSTRAP_SERVERS);
        String kafkaTopic = parameter.get(ConfigConstant.KAFKA_SINK_TOPIC);
        int kafkaParallelism = parameter.getInt(ConfigConstant.KAFKA_SINK_TOPIC_PARALLELISM);
        String kafkaUsername = parameter.get(ConfigConstant.KAFKA_SASL_USERNAME);
        String kafkaPassword = parameter.get(ConfigConstant.KAFKA_SASL_PASSWORD);
        Properties properties = getProperties(kafkaUsername, kafkaPassword);
        String name = "kafka-sink";
        KafkaRecordSerializationSchema<LogEvent> recordSerializationSchema = KafkaRecordSerializationSchema.builder()
                .setTopic(kafkaTopic).setValueSerializationSchema(new LogSchema()).build();
        KafkaSink<LogEvent> kafkaSink = KafkaSink.<LogEvent>builder()
                .setBootstrapServers(kafkaBootstrapServers)
                .setRecordSerializer(recordSerializationSchema)
                .setKafkaProducerConfig(properties)
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();
        stream.sinkTo(kafkaSink).setParallelism(kafkaParallelism).name(name).uid(name);
    }

    private static void sinkToElasticsearch(ParameterTool parameter, DataStream<LogEvent> dataSource) {
        List<HttpHost> esHttpHosts = ElasticsearchUtil.getEsAddresses(parameter.get(ConfigConstant.ELASTICSEARCH_HOSTS));
        int bulkMaxActions = parameter.getInt(ConfigConstant.ELASTICSEARCH_BULK_FLUSH_MAX_ACTIONS, 5000);
        int bulkMaxSize = parameter.getInt(ConfigConstant.ELASTICSEARCH_BULK_FLUSH_MAX_SIZE_MB, 5);
        int intervalMillis = parameter.getInt(ConfigConstant.ELASTICSEARCH_BULK_FLUSH_INTERVAL_MS, 1000);
        int esSinkParallelism = parameter.getInt(ConfigConstant.ELASTICSEARCH_SINK_PARALLELISM);
        String indexPostfix = parameter.get(ConfigConstant.ELASTICSEARCH_INDEX_POSTFIX, "");

        String name = "ES-sink";
        Sink<LogEvent> esSink = new Elasticsearch7SinkBuilder<LogEvent>()
                .setHosts(esHttpHosts.toArray(new HttpHost[0]))
                .setBulkFlushMaxActions(bulkMaxActions) // Instructs the sink to emit after every element, otherwise they would be buffered
                .setBulkFlushMaxSizeMb(bulkMaxSize)
                .setBulkFlushInterval(intervalMillis)
                .setBulkFlushBackoffStrategy(FlushBackoffType.EXPONENTIAL, 3, 1000)
                .setEmitter(
                        (element, context, indexer) ->
                                indexer.add(ElasticsearchUtil.createIndexRequest(element, indexPostfix)))
                .build();
        dataSource.sinkTo(esSink).setParallelism(esSinkParallelism).name(name).uid(name);
    }

    private static void sinkToRedis(ParameterTool parameter, DataStream<LogEvent> dataSource) {
        // save statistic information in redis
        int windowTime = parameter.getInt(ConfigConstant.REDIS_WINDOW_TIME_SECONDS);
        int windowCount = parameter.getInt(ConfigConstant.REDIS_WINDOW_TRIGGER_COUNT);
        int redisSinkParallelism = parameter.getInt(ConfigConstant.REDIS_SINK_PARALLELISM);
        String name = "redis-agg-log";
        DataStream<LogStatWindowResult> keyedStream = dataSource.keyBy((KeySelector<LogEvent, String>) LogEvent::getIndex)
                .window(TumblingProcessingTimeWindows.of(Time.seconds(windowTime)))
                .trigger(new CountTriggerWithTimeout<>(windowCount, TimeCharacteristic.ProcessingTime))
                .aggregate(new LogStatAggregateFunction(), new LogStatWindowFunction())
                .setParallelism(redisSinkParallelism).name(name).uid(name);
        String sinkName = "redis-sink";
        keyedStream.addSink(new RedisAggSinkFunction()).setParallelism(redisSinkParallelism).name(sinkName).uid(sinkName);
    }

    private static Properties getProperties(String kafkaUsername, String kafkaPassword) {
        Properties properties = new Properties();
        if (!StringUtil.isEmpty(kafkaUsername) && !StringUtil.isEmpty(kafkaPassword)) {
            properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
            properties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
            String jaasTemplate = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";";
            String jaasCfg = String.format(jaasTemplate, kafkaUsername, kafkaPassword);
            properties.put(SaslConfigs.SASL_JAAS_CONFIG, jaasCfg);
        }
        return properties;
    }

    private static void showConf(ParameterTool parameter) {
        logger.info("Show " + parameter.getNumberOfParameters() + " config parameters");
        Configuration configuration = parameter.getConfiguration();
        Map<String, String> map = configuration.toMap();
        for (String key : map.keySet()) {
            logger.info(key + ":" + map.get(key));
        }
    }
}
