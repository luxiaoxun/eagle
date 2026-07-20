package com.alarm.eagle.cep.example;

import com.alarm.eagle.cep.DynamicCEP;
import com.alarm.eagle.cep.processor.DefaultPatternProcessor;
import com.alarm.eagle.cep.processor.PatternProcessor;
import com.alarm.eagle.cep.processor.PatternProcessorDiscovererFactory;
import com.alarm.eagle.cep.processor.ApiPatternProcessorDiscovererFactory;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Eagle Dynamic CEP 测试示例
 *
 * <p>完全自包含测试 — 不需要 Kafka、不需要 MySQL：
 * <ol>
 *   <li>使用 {@code initialProcessors} 模式，规则内嵌在代码中</li>
 *   <li>使用 {@code env.fromCollection()} 生成模拟事件流</li>
 *   <li>使用 EventTime + WatermarkStrategy</li>
 *   <li>通过 {@code DynamicCEP.dynamicPatterns()} 执行 CEP 匹配</li>
 * </ol>
 *
 * <p>测试场景覆盖 4 条规则：
 * <ul>
 *   <li>规则1 — 登录暴力破解：3次失败后5分钟内成功</li>
 *   <li>规则2 — 浏览到下单转化：浏览商品后30分钟内下单</li>
 *   <li>规则3 — 账户盗用：登录→改密→大额转账</li>
 *   <li>规则4 — 支付重试：失败(余额不足或超时)后2分钟内成功</li>
 * </ul>
 *
 * <p>运行方式：直接运行 main() 方法即可。
 */
public class CepTestDemo {

    // ======================== 规则 JSON 定义 ========================

    /** 规则1：登录暴力破解 — 3次登录失败后5分钟内登录成功 */
    private static final String RULE1_LOGIN_BRUTEFORCE = "{"
            + "\"name\":\"login_bruteforce\",\"type\":\"COMPOSITE\",\"version\":1,"
            + "\"nodes\":["
            + "  {\"name\":\"login_fail\",\"type\":\"ATOMIC\","
            + "   \"quantifier\":{\"consumingStrategy\":\"STRICT\",\"properties\":[\"LOOPING\"],"
            + "     \"times\":{\"from\":3,\"to\":3,\"windowTime\":null}},"
            + "   \"condition\":{\"type\":\"AVIATOR\",\"expression\":\"action == 'LOGIN_FAIL'\"}},"
            + "  {\"name\":\"login_success\",\"type\":\"ATOMIC\","
            + "   \"quantifier\":{\"consumingStrategy\":\"SKIP_TILL_NEXT\","
            + "     \"properties\":[\"SINGLE\"],\"times\":null},"
            + "   \"condition\":{\"type\":\"AVIATOR\",\"expression\":\"action == 'LOGIN_SUCCESS'\"}}"
            + "],"
            + "\"edges\":[{\"source\":\"login_fail\",\"target\":\"login_success\","
            + "   \"type\":\"SKIP_TILL_NEXT\"}],"
            + "\"window\":{\"type\":\"FIRST_AND_LAST\",\"size\":5,\"unit\":\"MINUTES\"},"
            + "\"afterMatchStrategy\":{\"type\":\"NO_SKIP\",\"patternName\":null}"
            + "}";

    /** 规则2：浏览→下单 — 浏览商品后30分钟内下单（同 user 的任意商品） */
    private static final String RULE2_VIEW_TO_ORDER = "{"
            + "\"name\":\"view_to_order\",\"type\":\"COMPOSITE\",\"version\":1,"
            + "\"nodes\":["
            + "  {\"name\":\"view_product\",\"type\":\"ATOMIC\","
            + "   \"quantifier\":{\"consumingStrategy\":\"SKIP_TILL_NEXT\","
            + "     \"properties\":[\"SINGLE\"],\"times\":null},"
            + "   \"condition\":{\"type\":\"AVIATOR\",\"expression\":\"action == 'VIEW_PRODUCT'\"}},"
            + "  {\"name\":\"place_order\",\"type\":\"ATOMIC\","
            + "   \"quantifier\":{\"consumingStrategy\":\"SKIP_TILL_NEXT\","
            + "     \"properties\":[\"SINGLE\"],\"times\":null},"
            + "   \"condition\":{\"type\":\"AVIATOR\","
            + "     \"expression\":\"action == 'PLACE_ORDER'\"}}"
            + "],"
            + "\"edges\":[{\"source\":\"view_product\",\"target\":\"place_order\","
            + "   \"type\":\"SKIP_TILL_NEXT\"}],"
            + "\"window\":{\"type\":\"FIRST_AND_LAST\",\"size\":30,\"unit\":\"MINUTES\"},"
            + "\"afterMatchStrategy\":{\"type\":\"SKIP_PAST_LAST_EVENT\",\"patternName\":null}"
            + "}";

    /** 规则3：账户盗用 — 登录→改密→大额转账(>50000) */
    private static final String RULE3_ACCOUNT_HIJACK = "{"
            + "\"name\":\"account_hijack\",\"type\":\"COMPOSITE\",\"version\":1,"
            + "\"nodes\":["
            + "  {\"name\":\"login\",\"type\":\"ATOMIC\","
            + "   \"quantifier\":{\"consumingStrategy\":\"SKIP_TILL_NEXT\","
            + "     \"properties\":[\"SINGLE\"],\"times\":null},"
            + "   \"condition\":{\"type\":\"AVIATOR\",\"expression\":\"action == 'LOGIN_SUCCESS'\"}},"
            + "  {\"name\":\"change_pwd\",\"type\":\"ATOMIC\","
            + "   \"quantifier\":{\"consumingStrategy\":\"SKIP_TILL_NEXT\","
            + "     \"properties\":[\"SINGLE\"],\"times\":null},"
            + "   \"condition\":{\"type\":\"AVIATOR\",\"expression\":\"action == 'CHANGE_PASSWORD'\"}},"
            + "  {\"name\":\"large_transfer\",\"type\":\"ATOMIC\","
            + "   \"quantifier\":{\"consumingStrategy\":\"SKIP_TILL_NEXT\","
            + "     \"properties\":[\"SINGLE\"],\"times\":null},"
            + "   \"condition\":{\"type\":\"AVIATOR\","
            + "     \"expression\":\"action == 'TRANSFER' && amount > 50000\"}}"
            + "],"
            + "\"edges\":["
            + "  {\"source\":\"login\",\"target\":\"change_pwd\",\"type\":\"SKIP_TILL_NEXT\"},"
            + "  {\"source\":\"change_pwd\",\"target\":\"large_transfer\","
            + "   \"type\":\"SKIP_TILL_NEXT\"}"
            + "],"
            + "\"window\":{\"type\":\"FIRST_AND_LAST\",\"size\":40,\"unit\":\"MINUTES\"},"
            + "\"afterMatchStrategy\":{\"type\":\"NO_SKIP\",\"patternName\":null}"
            + "}";

    /** 规则4：支付重试 — OR 条件（余额不足 OR 超时） */
    private static final String RULE4_PAYMENT_RETRY = "{"
            + "\"name\":\"payment_retry\",\"type\":\"COMPOSITE\",\"version\":1,"
            + "\"nodes\":["
            + "  {\"name\":\"payment_fail\",\"type\":\"ATOMIC\","
            + "   \"quantifier\":{\"consumingStrategy\":\"SKIP_TILL_NEXT\","
            + "     \"properties\":[\"SINGLE\"],\"times\":null},"
            + "   \"condition\":{\"type\":\"OR\",\"conditions\":["
            + "     {\"type\":\"AVIATOR\","
            + "      \"expression\":\"action == 'PAYMENT_FAIL' && reason == 'BALANCE_NOT_ENOUGH'\"},"
            + "     {\"type\":\"AVIATOR\","
            + "      \"expression\":\"action == 'PAYMENT_FAIL' && reason == 'TIMEOUT'\"}"
            + "   ]}},"
            + "  {\"name\":\"payment_success\",\"type\":\"ATOMIC\","
            + "   \"quantifier\":{\"consumingStrategy\":\"SKIP_TILL_NEXT\","
            + "     \"properties\":[\"SINGLE\"],\"times\":null},"
            + "   \"condition\":{\"type\":\"AVIATOR\","
            + "     \"expression\":\"action == 'PAYMENT_SUCCESS'\"}}"
            + "],"
            + "\"edges\":[{\"source\":\"payment_fail\",\"target\":\"payment_success\","
            + "   \"type\":\"SKIP_TILL_NEXT\"}],"
            + "\"window\":{\"type\":\"FIRST_AND_LAST\",\"size\":2,\"unit\":\"MINUTES\"},"
            + "\"afterMatchStrategy\":{\"type\":\"NO_SKIP\",\"patternName\":null}"
            + "}";

    // ======================== Main ========================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   Eagle Dynamic CEP Test Demo               ║");
        System.out.println("║   测试 4 条 CEP 规则的完整匹配流程             ║");
        System.out.println("╚══════════════════════════════════════════════╝\n");

        // ---- 1. 构建规则 ----
        List<PatternProcessor<UserBehaviorEvent>> initialRules = buildRulesStatic();
        System.out.println("Loaded " + initialRules.size() + " CEP rules:\n");

        // ---- 2. 创建 Flink 环境 ----
        StreamExecutionEnvironment env =
                StreamExecutionEnvironment.createLocalEnvironment();
        env.setParallelism(1);

        // ---- 3. 生成测试事件 ----
        List<UserBehaviorEvent> testEvents = generateTestEventsStatic();
        System.out.println("Generated " + testEvents.size() + " test events:\n");

        DataStream<UserBehaviorEvent> eventStream = env
                .fromCollection(testEvents)
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<UserBehaviorEvent>forBoundedOutOfOrderness(
                                        Duration.ofSeconds(10))
                                .withTimestampAssigner(
                                        (event, ts) -> event.getEventTime()));

        // ---- 4. 创建 DiscovererFactory (initialProcessors 模式) ----
        PatternProcessorDiscovererFactory<UserBehaviorEvent> discovererFactory =
                new ApiPatternProcessorDiscovererFactory<>(
                        "http://localhost:9080/eagle-api",
                        initialRules,  // ← 初始规则
                        30000L);

        // ---- 5. 执行 Dynamic CEP ----
        SingleOutputStreamOperator<String> cepResults = DynamicCEP.dynamicPatterns(
                eventStream,
                discovererFactory,
                false,  // EventTime
                TypeInformation.of(String.class));

        cepResults.print("CEP-MATCH").setParallelism(1);

        // ---- 6. 执行 ----
        System.out.println("\n>>> Starting Flink job...\n");
        env.execute("Eagle Dynamic CEP Test Demo");

        System.out.println("\n>>> Job completed.");
    }

    // ======================== 构建初始规则 ========================

    @SuppressWarnings("unchecked")
    public static List<PatternProcessor<UserBehaviorEvent>> buildRulesStatic() {
        List<PatternProcessor<UserBehaviorEvent>> rules = new ArrayList<>();

        // 规则1：登录暴力破解检测
        rules.add(new DefaultPatternProcessor<>(
                "rule_login_bruteforce", 1,
                RULE1_LOGIN_BRUTEFORCE,
                new DemoCepOutputFunction("rule_login_bruteforce", 1)));

        // 规则2：浏览到下单转化
        rules.add(new DefaultPatternProcessor<>(
                "rule_view_to_order", 1,
                RULE2_VIEW_TO_ORDER,
                new DemoCepOutputFunction("rule_view_to_order", 1)));

        // 规则3：账户盗用检测
        rules.add(new DefaultPatternProcessor<>(
                "rule_account_hijack", 1,
                RULE3_ACCOUNT_HIJACK,
                new DemoCepOutputFunction("rule_account_hijack", 1)));

        // 规则4：支付重试检测（OR 条件）
        rules.add(new DefaultPatternProcessor<>(
                "rule_payment_retry", 1,
                RULE4_PAYMENT_RETRY,
                new DemoCepOutputFunction("rule_payment_retry", 1)));

        for (PatternProcessor<UserBehaviorEvent> rule : rules) {
            System.out.println("  ✓ " + rule.getId() + " (v" + rule.getVersion() + ")");
        }
        return rules;
    }

    // ======================== 生成测试事件 ========================

    public static List<UserBehaviorEvent> generateTestEventsStatic() {
        List<UserBehaviorEvent> events = new ArrayList<>();
        long baseTime = System.currentTimeMillis();

        // --- 场景 A：Alice 登录暴力破解（应触发规则1）---
        // 3 次 LOGIN_FAIL + 1 次 LOGIN_SUCCESS，全在 5 分钟内
        String alice = "alice";
        System.out.println("  [Alice] 3xLOGIN_FAIL → LOGIN_SUCCESS (within 5min) → SHOULD match rule 1");
        events.add(new UserBehaviorEvent(alice, "LOGIN_FAIL", null, 0, null,
                baseTime + 1000));
        events.add(new UserBehaviorEvent(alice, "LOGIN_FAIL", null, 0, null,
                baseTime + 30_000));
        events.add(new UserBehaviorEvent(alice, "LOGIN_FAIL", null, 0, null,
                baseTime + 60_000));
        events.add(new UserBehaviorEvent(alice, "LOGIN_SUCCESS", null, 0, null,
                baseTime + 90_000));

        // --- 场景 B：Bob 浏览商品后下单（应触发规则2）---
        System.out.println("  [Bob] VIEW_PRODUCT A → PLACE_ORDER A (within 30min) → SHOULD match rule 2");
        events.add(new UserBehaviorEvent("bob", "VIEW_PRODUCT", "Product_A",
                0, null, baseTime + 2000));
        events.add(new UserBehaviorEvent("bob", "PLACE_ORDER", "Product_A",
                299.99, null, baseTime + 120_000));

        // --- 场景 C：Charlie 账户盗用（应触发规则3）---
        System.out.println("  [Charlie] LOGIN → CHANGE_PWD → TRANSFER(60000) → SHOULD match rule 3");
        events.add(new UserBehaviorEvent("charlie", "LOGIN_SUCCESS", null, 0, null,
                baseTime + 3000));
        events.add(new UserBehaviorEvent("charlie", "CHANGE_PASSWORD", null, 0, null,
                baseTime + 180_000));
        events.add(new UserBehaviorEvent("charlie", "TRANSFER", null, 60000, null,
                baseTime + 300_000));

        // --- 场景 D：Dave 支付失败后重试成功（应触发规则4）---
        System.out.println("  [Dave] PAYMENT_FAIL(BALANCE) → PAYMENT_SUCCESS (within 2min) → SHOULD match rule 4");
        events.add(new UserBehaviorEvent("dave", "PAYMENT_FAIL", null, 1000,
                "BALANCE_NOT_ENOUGH", baseTime + 4000));
        events.add(new UserBehaviorEvent("dave", "PAYMENT_SUCCESS", null, 1000, null,
                baseTime + 70_000));

        // --- 场景 E：Eve 不应该触发任何规则 ---
        // 只有 2 次 LOGIN_FAIL（不够3次），不会触发规则1
        System.out.println("  [Eve] 2xLOGIN_FAIL → LOGIN_SUCCESS → should NOT match (only 2 fails)");
        events.add(new UserBehaviorEvent("eve", "LOGIN_FAIL", null, 0, null,
                baseTime + 5000));
        events.add(new UserBehaviorEvent("eve", "LOGIN_FAIL", null, 0, null,
                baseTime + 40_000));
        events.add(new UserBehaviorEvent("eve", "LOGIN_SUCCESS", null, 0, null,
                baseTime + 80_000));

        // --- 场景 F：Frank 超时（应不触发规则1）---
        // 3 次 LOGIN_FAIL，但 LOGIN_SUCCESS 在 10 分钟后（超过 5 分钟窗口）
        System.out.println("  [Frank] 3xLOGIN_FAIL → LOGIN_SUCCESS after 10min → should NOT match (timeout)");
        events.add(new UserBehaviorEvent("frank", "LOGIN_FAIL", null, 0, null,
                baseTime + 2000));
        events.add(new UserBehaviorEvent("frank", "LOGIN_FAIL", null, 0, null,
                baseTime + 5_000));
        events.add(new UserBehaviorEvent("frank", "LOGIN_FAIL", null, 0, null,
                baseTime + 10_000));
        events.add(new UserBehaviorEvent("frank", "LOGIN_SUCCESS", null, 0, null,
                baseTime + 600_000 + 10_000)); // 10分钟后

        System.out.println();
        return events;
    }
}
