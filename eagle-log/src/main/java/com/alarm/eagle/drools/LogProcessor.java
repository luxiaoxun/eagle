package com.alarm.eagle.drools;

import com.alarm.eagle.log.LogEvent;
import com.alarm.eagle.rule.Rule;
import com.alarm.eagle.rule.RuleBase;

import java.util.List;

/**
 * Created by luxiaoxun on 2020/01/29.
 */
public interface LogProcessor {
    String LOG_PKG = "log-rules";

    List<LogEvent> execute(String msg);

    List<LogEvent> execute(LogEvent entry);

    boolean loadRules(RuleBase ruleBase);

    boolean addRule(Rule rule);

    boolean removeRule(Rule rule);

    void destroy();
}
