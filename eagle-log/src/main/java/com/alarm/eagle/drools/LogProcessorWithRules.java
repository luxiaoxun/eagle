package com.alarm.eagle.drools;

import com.alarm.eagle.log.LogEntry;
import com.alarm.eagle.rule.Rule;
import com.alarm.eagle.rule.RuleBase;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.drools.core.impl.KnowledgeBaseImpl;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.utils.KieHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by luxiaoxun on 2020/01/29.
 */
public class LogProcessorWithRules implements LogProcessor {
    private static final Logger logger = LoggerFactory.getLogger(LogProcessorWithRules.class);

    private KieBase kieBase = null;
    private KieSession kieSession = null;
    private String rulePackage = null;

    public LogProcessorWithRules(String pkg) {
        rulePackage = pkg;
    }

    public LogProcessorWithRules(String pkg, RuleBase ruleBase) {
        rulePackage = pkg;
        KieHelper kieHelper = new KieHelper();
        if (ruleBase != null && !ruleBase.getRules().isEmpty()) {
            for (Rule rule : ruleBase.getRules()) {
                kieHelper.addResource(ResourceFactory.newByteArrayResource(rule.getScript().getBytes(StandardCharsets.UTF_8)), ResourceType.DRL);
            }
        }
        KieBaseConfiguration config = KieServices.Factory.get().newKieBaseConfiguration();
        config.setOption(EventProcessingOption.STREAM);
        kieBase = kieHelper.build();
        kieSession = kieBase.newKieSession();
    }

    @Override
    public List<LogEntry> execute(String msg) {
        LogEntry entry = new LogEntry((JsonObject) JsonParser.parseString(msg));
        return execute(entry);
    }

    @Override
    public List<LogEntry> execute(LogEntry entry) {
        List<LogEntry> result = new LinkedList<>();
        try {
            if (kieSession != null) {
                kieSession.setGlobal("LOG", logger);
                kieSession.insert(entry);
                kieSession.fireAllRules();
                for (Object obj : kieSession.getObjects()) {
                    result.add((LogEntry) obj);
                }
            }
        } catch (Exception ex) {
            logger.warn("Process log error: {}", ex.toString());
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
        kieBase = ksHelper.createKieBase();
        kieSession = kieBase.newKieSession();
        return true;
    }

    @Override
    public boolean addRule(Rule rule) {
        if (kieBase != null) {
            org.kie.api.definition.rule.Rule kieBaseRule = kieBase.getRule(rulePackage, rule.getName());
            if (kieBaseRule != null && kieBaseRule.getName().equals(rule.getName())) {
                logger.info("Rule {} already exist", rule.getName());
            } else {
                KnowledgeBuilder kb = KnowledgeBuilderFactory.newKnowledgeBuilder();
                kb.add(ResourceFactory.newByteArrayResource(rule.getScript().getBytes(StandardCharsets.UTF_8)), ResourceType.DRL);
                KnowledgeBaseImpl kieBaseImpl = (KnowledgeBaseImpl) kieBase;
                kieBaseImpl.addPackages(kb.getKnowledgePackages());
                kieSession = kieBase.newKieSession();
            }
            logger.info("Add rule {} successfully", rule.getName());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean removeRule(Rule rule) {
        if (kieBase != null) {
            kieBase.removeRule(rulePackage, rule.getName());
            kieSession = kieBase.newKieSession();
            logger.info("Remove rule {} successfully", rule.getName());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void destroy() {
        if (kieSession != null) {
            kieSession.destroy();
        }
    }
}
