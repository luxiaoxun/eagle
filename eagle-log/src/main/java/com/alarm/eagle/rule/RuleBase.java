package com.alarm.eagle.rule;

import com.alarm.eagle.util.JsonUtil;
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

    public RuleBase(List<Rule> ruleList) {
        rules = ruleList;
        hash = Md5Util.MD5(JsonUtil.encode(ruleList));
        name = "rules-" + hash;
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
        return JsonUtil.encode(this);
    }
}
