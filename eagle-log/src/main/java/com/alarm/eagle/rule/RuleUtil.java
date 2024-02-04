package com.alarm.eagle.rule;

import com.alarm.eagle.response.Response;
import com.alarm.eagle.util.HttpUtil;
import com.alarm.eagle.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuleUtil {
    private static final Logger logger = LoggerFactory.getLogger(RuleUtil.class);

    public static RuleBase getRules(String ruleUrl) {
        RuleBase ruleBase = null;
        try {
            String content = HttpUtil.doGet(ruleUrl);
//            String content = HttpUtil.doGetMock(ruleUrl);
            if (content == null) {
                logger.error("Failed to get rules from url {}", ruleUrl);
                return ruleBase;
            }

            Response response = JsonUtil.decode(content, Response.class);
            if (response == null || response.getData() == null || !response.getCode().equals("200")) {
                logger.error("Failed to get rules from url {}", ruleUrl);
                return ruleBase;
            }

            JsonArray resJson = JsonParser.parseString(JsonUtil.encode(response.getData())).getAsJsonArray();
            if (resJson == null) {
                logger.error("Failed to parse json:{}", content);
                return ruleBase;
            }

            ruleBase = RuleBase.createRuleBase(resJson);

        } catch (Exception e) {
            logger.error("Exception occurs:", e);
        }
        return ruleBase;
    }

}
