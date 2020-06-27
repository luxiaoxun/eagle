package com.alarm.eagle.rule;

import com.alarm.eagle.util.Md5Util;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

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

    public static RuleBase createRuleBase(JSONArray jsonArray) {
        RuleBase ruleBase = new RuleBase();
        for (Object obj : jsonArray) {
            JSONObject job = (JSONObject) obj;
            Rule rule = new Rule();
            rule.setId(job.getString("id"));
            rule.setType(job.getString("type"));
            rule.setScript(job.getString("script"));
            rule.setState(job.getString("state"));
            ruleBase.rules.add(rule);
        }
        String str = ruleBase.toString();
        ruleBase.hash = Md5Util.getMd5(str);
        ruleBase.name = "rules-" + ruleBase.hash;
        return ruleBase;
    }

    public static RuleBase createRuleBase(JSONArray jsonArray, String name) {
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
