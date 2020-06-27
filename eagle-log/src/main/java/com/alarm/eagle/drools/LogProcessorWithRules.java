package com.alarm.eagle.drools;

import com.alarm.eagle.log.LogEntry;
import com.alarm.eagle.rule.RuleBase;

import com.alibaba.fastjson.JSONObject;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by luxiaoxun on 2020/01/29.
 */
public class LogProcessorWithRules implements LogProcessor {
    private static final Logger logger = LoggerFactory.getLogger(LogProcessorWithRules.class);

    private KieBase kieBase = null;
    private boolean isEmptyKie = true;
    private String rulePackage = null;

    public LogProcessorWithRules(String pkg) {
        rulePackage = pkg;
    }

    @Override
    public List<LogEntry> execute(String msg) {
        LogEntry entry = new LogEntry((JSONObject) JSONObject.parse(msg));
        return execute(entry);
    }

    @Override
    public List<LogEntry> execute(LogEntry entry) {
        List<LogEntry> result = new LinkedList<>();
        KieSession kSession = kieBase.newKieSession();
        try {
            if (!isEmptyKie) {
                kSession.setGlobal("LOG", logger);
            }
            kSession.insert(entry);
            kSession.fireAllRules();
            for (Object obj : kSession.getObjects()) {
                result.add((LogEntry) obj);
            }
        } catch (Exception ex) {
            logger.warn("Process log error, ex:{}", ex);
        } finally {
            kSession.dispose();
        }
        return result;
    }

    @Override
    public boolean loadRules(RuleBase rb) {
        KieSessionHelper ksHelper = new KieSessionHelper(rulePackage);
        if (!ksHelper.compileRules(rb).isEmpty()) {
            logger.error("Failed to load rules, hash:{}", rb.getHash());
            return false;
        }
        isEmptyKie = rb.getRules().isEmpty();
        kieBase = ksHelper.createKieBase();
        return true;
    }
}
