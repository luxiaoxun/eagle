package com.alarm.eagle.api.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.alarm.eagle.api.constants.Constant;
import com.alarm.eagle.bean.AlertPolicy;
import com.alarm.eagle.bean.DataSink;
import com.alarm.eagle.util.JsonUtil;
import freemarker.template.TemplateException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import freemarker.template.Configuration;
import freemarker.template.Template;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by luxiaoxun on 18/1/17.
 */
@Component
public class TemplateEngine implements ApplicationContextInitializer {

    @Resource
    Configuration configuration;

    private Map<String, Object> defaultModel = new HashMap<>();

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
        defaultModel.put("ALERT_SYSTEM_NAME", Constant.ALERT_SYSTEM_NAME);
    }

    public Map<String, Object> getModel() {
        return new HashMap<>(defaultModel);
    }

    public Map<String, Object> getModel(AlertPolicy alertPolicy, DataSink dataSink) {
        Map<String, Object> model = new HashMap<>(defaultModel);
        model.put("alertPolicy", alertPolicy);
        model.put("policy", alertPolicy.getPolicy());
        model.put("dataSink", dataSink);
        model.put("data", JsonUtil.decode(dataSink.getData(), new TypeReference<Map<String, Object>>(){}));
        return model;
    }

    public Template getStringTemplate(String template) throws IOException {
        return new Template("", new StringReader(template), configuration);
    }

    public Template getTemplate(String path) throws IOException{
        return configuration.getTemplate(path);
    }

    public String render(Template template, Map<String, Object> model) throws TemplateException,IOException{
        Writer out = new StringWriter();
        template.process(model, out);
        return out.toString();
    }
}
