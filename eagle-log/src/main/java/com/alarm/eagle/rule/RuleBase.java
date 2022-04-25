package com.alarm.eagle.rule;

import com.alarm.eagle.util.Md5Util;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luxiaoxun on 2020/01/27.
 */
public class RuleBase implements Serializable {
    private static final long serialVersionUID = -6249685986229931397L;
    private List<Rule> rules = new ArrayList<>();
    private String name = "Empty-Name";
    private String hash = "";

    public static RuleBase createRuleBase(JsonArray jsonArray) {
        RuleBase ruleBase = new RuleBase();
        for (JsonElement obj : jsonArray) {
            JsonObject job = (JsonObject) obj;
            Rule rule = new Rule();
            rule.setId(job.get("id").getAsString());
            rule.setType(job.get("type").getAsString());
            rule.setScript(job.get("script").getAsString());
            rule.setState(job.get("state").getAsString());
            ruleBase.rules.add(rule);
        }
        String str = ruleBase.toString();
        ruleBase.hash = Md5Util.getMd5(str);
        ruleBase.name = "rules-" + ruleBase.hash;
        return ruleBase;
    }

    public static RuleBase createRuleBase(JsonArray jsonArray, String name) {
        RuleBase ruleBase = createRuleBase(jsonArray);
        ruleBase.name = name;
        return ruleBase;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public String toString() {
        return "RuleBase{" +
                "rules=" + rules +
                ", name='" + name + '\'' +
                ", hash='" + hash + '\'' +
                '}';
    }
}
