package com.alarm.eagle.drools;

import com.alarm.eagle.rule.Rule;
import com.alarm.eagle.rule.RuleBase;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.builder.model.KieSessionModel;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luxiaoxun on 2020/01/29.
 */
public class KieSessionHelper {
    private static final Logger logger = LoggerFactory.getLogger(KieSessionHelper.class);

    private String rulePackage = null;
    private KieServices kieServices = null;

    public KieSessionHelper(final String pkg) {
        rulePackage = pkg;
        kieServices = KieServices.Factory.get();
    }

    public KieBase createKieBase() {
        return kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId()).getKieBase();
    }

    //compile one rules script
    private List<String> compileRule(String script) {
        KieModuleModel kieModuleModel = kieServices.newKieModuleModel();

        KieBaseModel kieBaseModel = kieModuleModel.newKieBaseModel(rulePackage)
                .setDefault(true)
                .setEqualsBehavior(EqualityBehaviorOption.EQUALITY)
                .setEventProcessingMode(EventProcessingOption.STREAM);

        kieBaseModel.newKieSessionModel("ruleKieSession")
                .setDefault(true)
                .setType(KieSessionModel.KieSessionType.STATEFUL)
                .setClockType(ClockTypeOption.get("realtime"));

        KieFileSystem kfs = kieServices.newKieFileSystem();
        kfs.writeKModuleXML(kieModuleModel.toXML());
        kfs.write("src/main/resources/" + rulePackage + "/tmp.drl", script);
        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();
        Results results = kieBuilder.getResults();

        List<String> errorMsgs = new ArrayList<>();
        if (results.hasMessages(Message.Level.ERROR)) {
            for (Message msg : results.getMessages(Message.Level.ERROR)) {
                logger.error("Compile rule error: {}", msg.getText());
                errorMsgs.add(msg.getText());
            }
        }
        return errorMsgs;
    }

    //compile rules base
    public List<String> compileRules(RuleBase rb) {
        KieModuleModel kieModuleModel = kieServices.newKieModuleModel();

        KieBaseModel kieBaseModel = kieModuleModel.newKieBaseModel(rulePackage)
                .setDefault(true)
                .setEqualsBehavior(EqualityBehaviorOption.EQUALITY)
                .setEventProcessingMode(EventProcessingOption.STREAM);

        kieBaseModel.newKieSessionModel("ruleKieSession")
                .setDefault(true)
                .setType(KieSessionModel.KieSessionType.STATEFUL)
                .setClockType(ClockTypeOption.get("realtime"));

        KieFileSystem kfs = kieServices.newKieFileSystem();
        kfs.writeKModuleXML(kieModuleModel.toXML());
        for (Rule rule : rb.getRules()) {
            kfs.write("src/main/resources/" + rule.getType() + "/" + rule.getId() + ".drl", rule.getScript());
        }

        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();
        Results results = kieBuilder.getResults();
        List<String> errorMsgs = new ArrayList<>();
        if (results.hasMessages(Message.Level.ERROR)) {
            for (Message msg : results.getMessages(Message.Level.ERROR)) {
                logger.error("Compile rule error: {}", msg.getText());
                errorMsgs.add(msg.getText());
            }
        }
        return errorMsgs;
    }

}

