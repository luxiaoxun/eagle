package com.alarm.eagle.rule;

import com.alarm.eagle.util.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by luxiaoxun on 2020/01/27.
 */
public class RuleSourceFunction extends RichSourceFunction<RuleBase> {
    private static final Logger logger = LoggerFactory.getLogger(RuleSourceFunction.class);

    private boolean isRunning = true;
    private String ruleUrl;

    public RuleSourceFunction(String url) {
        this.ruleUrl = url;
    }

    @Override
    public void run(SourceContext sourceContext) throws Exception {
        while (isRunning) {
            logger.info("Http to get rules from " + ruleUrl);
//            String content = HttpUtil.doGet(ruleUrl);
            String content = HttpUtil.doGetMock(ruleUrl);
            if (content == null) {
                logger.error("Failed to get rules from url {}", ruleUrl);
                return;
            }

            JSONArray resJson = JSON.parseArray(content);
            if (resJson == null) {
                logger.error("Failed to parse json:{}", content);
                return;
            }

            try {
                RuleBase ruleBase = RuleBase.createRuleBase(resJson);
                sourceContext.collect(ruleBase);
            } catch (Exception e) {
                logger.error("Exception occurs:", e);
            }

            Thread.sleep(60000);
        }
    }

    @Override
    public void cancel() {
        isRunning = false;
    }
}
