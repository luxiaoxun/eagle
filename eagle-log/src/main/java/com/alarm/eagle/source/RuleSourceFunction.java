package com.alarm.eagle.source;

import com.alarm.eagle.rule.RuleBase;
import com.alarm.eagle.rule.RuleUtil;
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
            RuleBase ruleBase = RuleUtil.getMockRules(ruleUrl);
            if (ruleBase != null) {
                sourceContext.collect(ruleBase);
            }
            Thread.sleep(60000);
        }
    }

    @Override
    public void cancel() {
        isRunning = false;
    }
}
