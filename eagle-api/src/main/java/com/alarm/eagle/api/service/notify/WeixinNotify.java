package com.alarm.eagle.api.service.notify;

import com.alarm.eagle.api.config.WeixinConfig;
import com.alarm.eagle.api.util.HttpUtil;
import com.alarm.eagle.api.util.TemplateEngine;
import com.alarm.eagle.bean.AlertPolicy;
import com.alarm.eagle.bean.AlertUser;
import com.alarm.eagle.bean.DataSink;
import com.alarm.eagle.constants.AlertConstant.*;
import com.alarm.eagle.util.JsonUtil;
import freemarker.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Created by luxiaoxun on 18/1/17.
 */
@Component
public class WeixinNotify implements Notify {

    private static final Logger logger = LoggerFactory.getLogger(WeixinNotify.class);

    private static final String WEIXIN_DEFAULT_TEMPALTE_NAME = "weixinNotify.ftl";

    private static final String WEIXIN_PUSH_URL = "/send.json";

    @Resource
    private WeixinConfig weixinConfig;

    @Resource
    private TemplateEngine engine;

    @Override
    public AlertType getAlertType() {
        return AlertType.Weixin;
    }

    private String getText(AlertPolicy alertPolicy, DataSink dataSink) throws Exception{
        Template template;
        if(StringUtils.isNotBlank(alertPolicy.getTxtTemplate())) {
            template = engine.getStringTemplate(alertPolicy.getTxtTemplate());
        }else {
            template = engine.getTemplate(WEIXIN_DEFAULT_TEMPALTE_NAME);
        }
        Map<String, Object> model = engine.getModel(alertPolicy, dataSink);
        return engine.render(template, model);
    }

    private String getWeixinContent(String toUser, String content) {
        Map<String, Object> weixin = new HashMap<>();
        weixin.put("touser", toUser);
        weixin.put("toparty", "");
        weixin.put("totag", "");
        weixin.put("msgtype", "text");
        weixin.put("agentid", weixinConfig.getAppId());
        Map<String, String> text = new HashMap<>();
        text.put("content", content);
        weixin.put("text", text);
        weixin.put("safe", 0);
        return  JsonUtil.encode(weixin);
    }

    @Override
    public void notify(AlertPolicy alertPolicy, DataSink dataSink) {
        String content = "";
        try {
            content = getText(alertPolicy, dataSink);
        }catch (Exception e) {
            logger.error("metric=eagle-weixinContentError||error=", e);
            return;
        }

        StringJoiner joiner = new StringJoiner("|");
        for(AlertUser alertUser : alertPolicy.getUserList()) {
           joiner.add(alertUser.getWeixinName());
        }

        String message = getWeixinContent(joiner.toString(), content);
        send(message);
    }

    public void send(String content) {
        try {
            String result = HttpUtil.post(weixinConfig.getUrl() + WEIXIN_PUSH_URL, new String[]{"params", "key"},
                    new Object[]{content, weixinConfig.getSendKey()});
            logger.info("metric=eagle-weixin||content={}||result={}", content, result);
        }catch (Exception e) {
            logger.error("metric=eagle-weixinSendError||params={}||error=", content, e);
        }
    }
}
