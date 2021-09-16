package com.alarm.eagle.drools;

import com.alarm.eagle.log.LogEntry;
import com.alarm.eagle.rule.Rule;
import com.alarm.eagle.rule.RuleBase;

import java.util.List;

/**
 * Created by luxiaoxun on 2020/01/29.
 */
public interface LogProcessor {
    String LOG_PKG = "logrules";

    List<LogEntry> execute(String msg);

    List<LogEntry> execute(LogEntry entry);

    boolean loadRules(RuleBase ruleBase);

    boolean addRule(Rule rule);

    boolean removeRule(Rule rule);

    void destroy();
}
