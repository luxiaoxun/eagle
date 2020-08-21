package com.alarm.eagle.log;

import com.alarm.eagle.rule.RuleBase;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.util.OutputTag;

public class Descriptors {
    // a map descriptor to store the rules
    public static final MapStateDescriptor<String, RuleBase> ruleStateDescriptor = new MapStateDescriptor<>(
            "rules-state", BasicTypeInfo.STRING_TYPE_INFO, TypeInformation.of(new TypeHint<RuleBase>() {
    }));

    public static final OutputTag<LogEntry> kafkaOutputTag = new OutputTag<LogEntry>("log-kafka-output",
            TypeInformation.of(LogEntry.class)) {
    };
}
