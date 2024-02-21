package com.alarm.eagle.source;

import com.alarm.eagle.rule.RuleHelper;
import com.alarm.eagle.util.HttpUtil;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuleSourceFunction extends RichSourceFunction<String> {
    private static final Logger logger = LoggerFactory.getLogger(RuleSourceFunction.class);

    private boolean isRunning = true;
    private String ruleUrl;

    public RuleSourceFunction(String url) {
        this.ruleUrl = url;
    }

    @Override
    public void run(SourceContext sourceContext) throws Exception {
        while (isRunning) {
            try {
                logger.info("Http to get alarm rules from " + ruleUrl);
                String content = RuleHelper.getMockRules(ruleUrl);
                if (content == null) {
                    logger.error("Failed to get alarm rules from url {}", ruleUrl);
                    return;
                }
                sourceContext.collect(content);
                Thread.sleep(60000);
            } catch (Exception ex) {
                logger.error("Get rule source exception: " + ex.toString());
            }
        }
    }

    @Override
    public void cancel() {
        isRunning = false;
    }
}
