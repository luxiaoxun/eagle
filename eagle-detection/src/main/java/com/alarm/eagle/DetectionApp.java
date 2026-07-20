package com.alarm.eagle;

import com.alarm.eagle.cep.DynamicCEP;
import com.alarm.eagle.cep.processor.DefaultPatternProcessor;
import com.alarm.eagle.cep.processor.PatternProcessor;
import com.alarm.eagle.cep.processor.PatternProcessorDiscovererFactory;
import com.alarm.eagle.cep.processor.ApiPatternProcessorDiscovererFactory;
import com.alarm.eagle.config.Constant;
import com.alarm.eagle.config.DetectionProperties;
import com.alarm.eagle.message.Transaction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.cep.functions.PatternProcessFunction;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.config.SaslConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Eagle Detection — Flink 实时欺诈检测作业
 *
 * <p>使用 Dynamic CEP 引擎，支持运行时热更新规则：
 * <ol>
 *   <li>从 Kafka 消费交易事件流</li>
 *   <li>启动时从 eagle-api 同步加载 CEP 规则</li>
 *   <li>运行时通过 ApiPatternProcessorDiscoverer 后台轮询规则更新</li>
 *   <li>匹配结果输出到标准输出（dev）或 Kafka（prod）</li>
 * </ol>
 */
public class DetectionApp {
    private static final Logger logger = LoggerFactory.getLogger(DetectionApp.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        try {
            ParameterTool params = ParameterTool.fromArgs(args);
            ParameterTool parameter = DetectionProperties.getInstance(params).getParameter();
            showConf(parameter);

            // ---- 1. 创建 Flink 环境 ----
            StreamExecutionEnvironment env = getStreamExecutionEnvironment(parameter);

            // ---- 2. Kafka 数据源 ----
            DataStream<Transaction> transactions = getTransactionStream(env, parameter);

            // ---- 3. 启动时同步加载 CEP 规则 ----
            String apiBaseUrl = parameter.get(Constant.API_BASE_URL);
            List<PatternProcessor<Transaction>> initialRules =
                    fetchRulesFromApi(apiBaseUrl);
            logger.info("Loaded {} initial CEP rules from API", initialRules.size());

            // ---- 4. 创建 DiscovererFactory ----
            long pollIntervalMs = parameter.getLong(
                    Constant.CEP_RULE_POLL_INTERVAL_MS, 30000L);
            PatternProcessorDiscovererFactory<Transaction> discovererFactory =
                    new ApiPatternProcessorDiscovererFactory<>(
                            apiBaseUrl, initialRules, pollIntervalMs);

            // ---- 5. Dynamic CEP ----
            SingleOutputStreamOperator<String> cepResults = DynamicCEP.dynamicPatterns(
                    transactions,
                    discovererFactory,
                    false,  // EventTime
                    TypeInformation.of(String.class));

            // ---- 6. 输出 ----
            cepResults.print("CEP-ALERT").setParallelism(1).name("CEP Alert STDOUT");

            env.getConfig().setGlobalJobParameters(parameter);
            env.execute("eagle-detection");

        } catch (Exception e) {
            logger.error("DetectionApp error: ", e);
        }
    }

    // ======================== Kafka 数据源 ========================

    private static DataStream<Transaction> getTransactionStream(
            StreamExecutionEnvironment env, ParameterTool parameter) {
        String bootstrapServers = parameter.get(Constant.KAFKA_BOOTSTRAP_SERVERS);
        String groupId = parameter.get(Constant.KAFKA_GROUP_ID);
        String topic = parameter.get(Constant.KAFKA_TOPIC);
        int parallelism = parameter.getInt(Constant.KAFKA_TOPIC_PARALLELISM);
        int orderness = parameter.getInt(Constant.TRANSACTIONS_OUT_OF_ORDERNESS);
        String username = parameter.get(Constant.KAFKA_SASL_USERNAME, "");
        String password = parameter.get(Constant.KAFKA_SASL_PASSWORD, "");

        Properties props = new Properties();
        if (!username.isEmpty() && !password.isEmpty()) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
            props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
            props.put(SaslConfigs.SASL_JAAS_CONFIG,
                    String.format("org.apache.kafka.common.security.plain.PlainLoginModule " +
                            "required username=\"%s\" password=\"%s\";", username, password));
        }

        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(bootstrapServers)
                .setTopics(topic)
                .setGroupId(groupId)
                .setProperties(props)
                .setStartingOffsets(OffsetsInitializer.committedOffsets(
                        OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        return env.fromSource(source, WatermarkStrategy.noWatermarks(),
                        "Kafka-" + topic)
                .setParallelism(parallelism)
                .map(Transaction::fromString)
                .name("Transaction Parser")
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<Transaction>forBoundedOutOfOrderness(
                                        Duration.ofMillis(orderness))
                                .withTimestampAssigner(
                                        (txn, ts) -> txn.getEventTime()));
    }

    // ======================== 从 API 同步加载规则 ========================

    /**
     * 启动时同步调用 GET /cep/rules 加载 CEP 规则。
     * 确保规则在 Kafka 事件到达前已就绪。
     */
    private static List<PatternProcessor<Transaction>> fetchRulesFromApi(
            String apiBaseUrl) {
        List<PatternProcessor<Transaction>> rules = new ArrayList<>();
        try {
            String json = httpGet(apiBaseUrl + "/cep/rules");
            if (json == null || json.isEmpty()) {
                logger.warn("Empty response from CEP rules API, starting with no rules");
                return rules;
            }

            JsonNode root = mapper.readTree(json);
            JsonNode dataNode = root.has("data") ? root.get("data") : root;
            if (!dataNode.isArray()) return rules;

            for (JsonNode item : dataNode) {
                String ruleId = item.get("ruleId").asText();
                int version = item.get("version").asInt();
                String patternStr = item.get("pattern").asText();

                rules.add(new DefaultPatternProcessor<>(
                        ruleId, version, patternStr,
                        new DetectionAlertFunction(ruleId, version)));
                logger.info("  Loaded rule: {} (v{})", ruleId, version);
            }
        } catch (Exception e) {
            logger.error("Failed to fetch CEP rules from API, starting with no rules: {}",
                    e.getMessage());
        }
        return rules;
    }

    private static String httpGet(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) return null;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }
            conn.disconnect();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ======================== Flink 环境配置 ========================

    private static StreamExecutionEnvironment getStreamExecutionEnvironment(
            ParameterTool parameter) {
        StreamExecutionEnvironment env;
        int globalParallelism = parameter.getInt(Constant.FLINK_PARALLELISM);
        if (parameter.get(Constant.FLINK_MODE).equals(Constant.MODE_DEV)) {
            env = StreamExecutionEnvironment.createLocalEnvironment();
            globalParallelism = 1;
        } else {
            env = StreamExecutionEnvironment.getExecutionEnvironment();
        }
        env.setParallelism(globalParallelism);

        boolean enableCheckpoint = parameter.getBoolean(
                Constant.FLINK_ENABLE_CHECKPOINT, false);
        if (enableCheckpoint) {
            env.enableCheckpointing(60000L);
            CheckpointConfig config = env.getCheckpointConfig();
            config.setMinPauseBetweenCheckpoints(30000L);
            config.setCheckpointTimeout(10000L);
            config.setExternalizedCheckpointCleanup(
                    CheckpointConfig.ExternalizedCheckpointCleanup.DELETE_ON_CANCELLATION);
        }
        return env;
    }

    private static void showConf(ParameterTool parameter) {
        logger.info("Show {} config parameters", parameter.getNumberOfParameters());
        Configuration configuration = parameter.getConfiguration();
        for (String key : configuration.toMap().keySet()) {
            logger.info("  {} = {}", key, configuration.getString(key, ""));
        }
    }

    // ======================== 告警输出函数 ========================

    /**
     * 匹配结果输出函数，将 CEP 匹配到的序列格式化为告警字符串。
     */
    public static class DetectionAlertFunction
            extends PatternProcessFunction<Transaction, String> {

        private static final long serialVersionUID = 1L;
        private final String ruleId;
        private final int version;

        public DetectionAlertFunction(String ruleId, int version) {
            this.ruleId = ruleId;
            this.version = version;
        }

        @Override
        public void processMatch(
                Map<String, List<Transaction>> match,
                Context ctx, Collector<String> out) {
            StringBuilder sb = new StringBuilder();
            sb.append("[ALERT] rule=").append(ruleId)
                    .append(" v").append(version).append(" | ");

            for (Map.Entry<String, List<Transaction>> entry : match.entrySet()) {
                sb.append(entry.getKey()).append("=[");
                for (Transaction txn : entry.getValue()) {
                    sb.append("txnId=").append(txn.getTransactionId())
                            .append(",payer=").append(txn.getPayerId())
                            .append(",beneficiary=").append(txn.getBeneficiaryId())
                            .append(",amount=").append(txn.getPaymentAmount())
                            .append("; ");
                }
                sb.append("] ");
            }
            out.collect(sb.toString());
        }
    }
}
