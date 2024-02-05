package com.alarm.eagle.rule;

import com.alarm.eagle.response.Response;
import com.alarm.eagle.util.HttpUtil;
import com.alarm.eagle.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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

            Response<List<Rule>> response = JsonUtil.decode(content, new TypeReference<Response<List<Rule>>>() {
            });
            if (response == null || response.getData() == null || !response.getCode().equals("200")) {
                logger.error("Failed to get rules content: {}", content);
                return ruleBase;
            }

            List<Rule> ruleList = response.getData();
            ruleBase = new RuleBase(ruleList);

        } catch (Exception e) {
            logger.error("Exception occurs:", e);
        }
        return ruleBase;
    }

}
