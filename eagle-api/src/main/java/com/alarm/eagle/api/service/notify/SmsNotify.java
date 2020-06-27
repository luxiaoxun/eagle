package com.alarm.eagle.api.service.notify;

import com.alarm.eagle.api.config.SmsConfig;
import com.alarm.eagle.api.util.HttpUtil;
import com.alarm.eagle.api.util.TemplateEngine;
import com.alarm.eagle.api.bean.AlertPolicy;
import com.alarm.eagle.api.bean.AlertUser;
import com.alarm.eagle.api.bean.DataSink;
import com.alarm.eagle.constants.AlertConstant.*;
import freemarker.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.StringJoiner;


/**
 * Created by luxiaoxun on 18/1/17.
 */
@Component
public class SmsNotify implements Notify {

    private static final Logger logger = LoggerFactory.getLogger(SmsNotify.class);

    private static final String SMS_DEFAULT_TEMPALTE_NAME = "smsNotify.ftl";

    @Resource
    private SmsConfig smsConfig;

    @Resource
    private TemplateEngine engine;

    @Override
    public AlertType getAlertType() {
        return AlertType.SMS;
    }

    private String getText(AlertPolicy alertPolicy, DataSink dataSink) throws Exception{
        Template template;
        if(StringUtils.isNotBlank(alertPolicy.getTxtTemplate())) {
            template = engine.getStringTemplate(alertPolicy.getTxtTemplate());
        }else {
            template = engine.getTemplate(SMS_DEFAULT_TEMPALTE_NAME);
        }
        Map<String, Object> model = engine.getModel(alertPolicy, dataSink);
        return engine.render(template, model);
    }

    @Override
    public void notify(AlertPolicy alertPolicy, DataSink dataSink) {
        String content = null;
        try {
            content = getText(alertPolicy, dataSink);
        }catch (Exception e) {
            logger.error("metric=eagle-smsContentError||error=", e);
            return;
        }
        StringJoiner joiner = new StringJoiner(",");

        for(AlertUser alertUser : alertPolicy.getUserList()) {
            joiner.add(alertUser.getPhone());
        }
        send(content, joiner.toString());
    }

    public void send(String content, String phones) {
        if(StringUtils.isBlank(content) || StringUtils.isBlank(phones)) {
            return;
        }
        try{
            String result = HttpUtil.post(this.smsConfig.getUrl(), new String[]{"content","phones","source","token"},
                    new Object[]{content, phones, this.smsConfig.getSource(), this.smsConfig.getToken()});
            logger.info("metric=eagle-smsSendOk||phone={}||content={}||result={}", phones, content, result);
        }catch(Exception e) {
            logger.error("metric=eagle-smsSendFailed||phone={}||content={}", phones, content);
        }
    }
}
