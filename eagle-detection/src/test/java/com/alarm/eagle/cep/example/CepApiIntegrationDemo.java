package com.alarm.eagle.cep.example;

import com.alarm.eagle.cep.DynamicCEP;
import com.alarm.eagle.cep.processor.DefaultPatternProcessor;
import com.alarm.eagle.cep.processor.PatternProcessor;
import com.alarm.eagle.cep.processor.PatternProcessorDiscovererFactory;
import com.alarm.eagle.cep.processor.ApiPatternProcessorDiscovererFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Eagle Dynamic CEP API 集成示例
 *
 * <p>演示与 eagle-api 对接的完整流程：
 * <ol>
 *   <li><b>启动时同步加载</b>：通过 HTTP GET /cep/rules 从 eagle-api 拉取规则，
 *       转为 {@link PatternProcessor} 列表作为 initialProcessors（确保规则在事件到达前就绪）</li>
 *   <li><b>运行时轮询更新</b>：通过 {@link ApiPatternProcessorDiscovererFactory}
 *       定时轮询 API，检测规则变更后通过 OperatorEvent 热更新到算子</li>
 * </ol>
 *
 * <p>运行前提：
 * <ul>
 *   <li>eagle-api 已启动，监听 http://localhost:8080/eagle-api</li>
 *   <li>MySQL 中已执行 db_cep_rule.sql，规则数据已就绪</li>
 * </ul>
 *
 * <p>如果 API 不可达，自动降级为内嵌规则（CepTestDemo 模式）。
 */
public class CepApiIntegrationDemo {

    private static final String DEFAULT_API_URL = "http://localhost:8080/eagle-api";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        String apiBaseUrl = args.length > 0 ? args[0] : DEFAULT_API_URL;

        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   Eagle Dynamic CEP — API Integration Demo   ║");
        System.out.println("║   API: " + padRight(apiBaseUrl, 37) + "║");
        System.out.println("╚══════════════════════════════════════════════╝\n");

        // ============ 1. 启动时同步加载规则（模拟 DetectionApp 的 RuleSource 初始化） ============
        System.out.println(">>> Step 1: Loading initial rules from API...");
        List<PatternProcessor<UserBehaviorEvent>> initialRules =
                fetchRulesFromApi(apiBaseUrl);

        if (initialRules.isEmpty()) {
            System.out.println("⚠ API unavailable or no rules, falling back to embedded rules.");
            initialRules = CepTestDemo.buildRulesStatic();
        }
        System.out.println("    Loaded " + initialRules.size() + " rules:\n");
        for (PatternProcessor<UserBehaviorEvent> r : initialRules) {
            System.out.println("    ✓ " + r.getId() + " (v" + r.getVersion() + ")");
        }

        // ============ 2. 创建 Flink 环境 ============
        System.out.println("\n>>> Step 2: Creating Flink environment...");
        StreamExecutionEnvironment env =
                StreamExecutionEnvironment.createLocalEnvironment();
        env.setParallelism(1);

        // ============ 3. 构建事件流 ============
        List<UserBehaviorEvent> testEvents = CepTestDemo.generateTestEventsStatic();
        System.out.println("    Generated " + testEvents.size() + " test events");

        DataStream<UserBehaviorEvent> eventStream = env
                .fromCollection(testEvents)
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<UserBehaviorEvent>forBoundedOutOfOrderness(
                                        Duration.ofSeconds(10))
                                .withTimestampAssigner(
                                        (event, ts) -> event.getEventTime()));

        // ============ 4. 创建 DiscovererFactory ============
        // initialProcessors: 启动时从 API 加载的规则（确保首次事件到达时规则已就绪）
        // ApiPatternProcessorDiscoverer: 后台每 30s 轮询 API，检测变更后热更新
        PatternProcessorDiscovererFactory<UserBehaviorEvent> discovererFactory =
                new ApiPatternProcessorDiscovererFactory<>(
                        apiBaseUrl,
                        initialRules,   // ← 关键：启动时已加载的规则
                        30_000L);       // 30 秒轮询一次

        // ============ 5. 执行 Dynamic CEP ============
        SingleOutputStreamOperator<String> cepResults = DynamicCEP.dynamicPatterns(
                eventStream,
                discovererFactory,
                false,  // EventTime
                TypeInformation.of(String.class));

        cepResults.print("CEP-MATCH").setParallelism(1);

        // ============ 6. 执行 ============
        System.out.println("\n>>> Step 3: Starting Flink job...\n");
        env.execute("Eagle Dynamic CEP — API Integration Demo");

        System.out.println("\n>>> Job completed.");
    }

    // ======================== 从 API 同步加载规则 ========================

    /**
     * 启动时同步调用 GET /cep/rules 加载规则。
     * 对应 DetectionApp 中 RuleSourceFunction 启动时拉取初始规则的逻辑。
     */
    private static List<PatternProcessor<UserBehaviorEvent>> fetchRulesFromApi(
            String apiBaseUrl) {
        List<PatternProcessor<UserBehaviorEvent>> rules = new ArrayList<>();
        try {
            String json = httpGet(apiBaseUrl + "/cep/rules");
            if (json == null || json.isEmpty()) return rules;

            JsonNode root = mapper.readTree(json);
            // Response 结构: { "code": 0, "data": [...] }
            JsonNode dataNode = root.has("data") ? root.get("data") : root;
            if (!dataNode.isArray()) return rules;

            for (JsonNode item : dataNode) {
                String ruleId = item.get("ruleId").asText();
                int version = item.get("version").asInt();
                String patternStr = item.get("pattern").asText();

                // 使用通用的输出函数
                rules.add(new DefaultPatternProcessor<>(
                        ruleId, version, patternStr,
                        new DemoCepOutputFunction(ruleId, version)));
            }
        } catch (Exception e) {
            System.out.println("    ⚠ Failed to fetch rules from API: " + e.getMessage());
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

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}
