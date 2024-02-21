package com.alarm.eagle.source;

import java.util.concurrent.TimeUnit;

import com.alarm.eagle.config.Constant;
import com.alarm.eagle.functions.RuleDeserializer;
import com.alarm.eagle.rule.Rule;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;

public class RulesSource {
    private static final int RULES_STREAM_PARALLELISM = 1;

    public static SourceFunction<String> createRulesSource(ParameterTool parameter) {
        String ruleUrl = parameter.get(Constant.STREAM_RULE_URL);
        RuleSourceFunction ruleSourceFunction = new RuleSourceFunction(ruleUrl);
        return ruleSourceFunction;
    }

    public static DataStream<Rule> stringsStreamToRules(DataStream<String> ruleStrings) {
        return ruleStrings
                .flatMap(new RuleDeserializer())
                .name("Rule Source Deserialization")
                .setParallelism(RULES_STREAM_PARALLELISM)
                .assignTimestampsAndWatermarks(
                        new BoundedOutOfOrdernessTimestampExtractor<Rule>(Time.of(0, TimeUnit.MILLISECONDS)) {
                            @Override
                            public long extractTimestamp(Rule element) {
                                // Prevents connected data+update stream watermark stalling.
                                return Long.MAX_VALUE;
                            }
                        });
    }
}
