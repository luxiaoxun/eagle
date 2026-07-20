package com.alarm.eagle.cep.example;

import org.apache.flink.cep.functions.PatternProcessFunction;
import org.apache.flink.util.Collector;

import java.util.List;
import java.util.Map;

/**
 * Dynamic CEP 测试用输出函数 — 格式化匹配结果。
 *
 * <p>构造函数签名为 {@code (String ruleId, int version)}，
 * 以支持 JDBCPeriodicPatternProcessorDiscoverer 的反射实例化。
 */
public class DemoCepOutputFunction
        extends PatternProcessFunction<UserBehaviorEvent, String> {

    private static final long serialVersionUID = 1L;

    private final String ruleId;
    private final int version;

    public DemoCepOutputFunction(String ruleId, int version) {
        this.ruleId = ruleId;
        this.version = version;
    }

    @Override
    public void processMatch(
            Map<String, List<UserBehaviorEvent>> match,
            Context ctx,
            Collector<String> out) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════════╗\n");
        sb.append("║  ★ CEP Rule Matched!                        ║\n");
        sb.append("║  Rule: ").append(ruleId)
                .append(" (v").append(version).append(")\n");
        sb.append("╠══════════════════════════════════════════════╣\n");

        for (Map.Entry<String, List<UserBehaviorEvent>> entry :
                match.entrySet()) {
            sb.append("║  [").append(entry.getKey()).append("]:\n");
            for (UserBehaviorEvent event : entry.getValue()) {
                sb.append("║    → userId=").append(event.getUserId())
                        .append(", action=").append(event.getAction())
                        .append(", product=").append(event.getProduct())
                        .append(", amount=").append(event.getAmount());
                if (event.getReason() != null) {
                    sb.append(", reason=").append(event.getReason());
                }
                sb.append("\n");
            }
        }
        sb.append("╚══════════════════════════════════════════════╝");
        out.collect(sb.toString());
    }
}
