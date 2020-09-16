package com.alarm.eagle.api.service.notify;

import static com.alarm.eagle.api.constants.Constant.*;

import com.alarm.eagle.api.util.TemplateEngine;
import com.alarm.eagle.bean.AlertPolicy;
import com.alarm.eagle.bean.DataSink;
import com.alarm.eagle.constants.AlertConstant.*;
import freemarker.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by luxiaoxun on 18/1/17.
 */
@Component
public class MailNotify implements Notify {

    private static final Logger logger = LoggerFactory.getLogger(MailNotify.class);

    private static final String MAIL_DEFAULT_TEMPALTE_NAME = "mailNotify.ftl";

    @Resource
    private TemplateEngine engine;

    @Resource
    private JavaMailSender mailSender;

    @Override
    public AlertType getAlertType() {
        return AlertType.Mail;
    }

    private String getSubject(AlertPolicy alertPolicy) {
        return String.format("[%s][%s:%s]%s", ALERT_SYSTEM_NAME, ALERT_LEVEL_NAME, alertPolicy.getPolicy().getAlertLevel(), alertPolicy.getPolicy().getPolicyName());
    }

    private String getText(AlertPolicy alertPolicy, DataSink dataSink) throws Exception {
        Template template;
        if(StringUtils.isNotBlank(alertPolicy.getHtmlTemplate())) {
            template = engine.getStringTemplate(alertPolicy.getHtmlTemplate());
        }else if(StringUtils.isNotBlank(alertPolicy.getTxtTemplate())) {
            template = engine.getStringTemplate(alertPolicy.getTxtTemplate());
        }else {
            template = engine.getTemplate(MAIL_DEFAULT_TEMPALTE_NAME);
        }

        Map<String, Object> model = engine.getModel(alertPolicy, dataSink);
        return engine.render(template, model);
    }

    @Override
    public void notify(AlertPolicy alertPolicy, DataSink dataSink) {
        MimeMessage message = mailSender.createMimeMessage();
        List<String> mailList= alertPolicy.getUserList().stream().map(alertUser -> alertUser.getMail()).collect(Collectors.toList());
        String[] recipients = new String[mailList.size()];
        mailList.toArray(recipients);
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setSubject(getSubject(alertPolicy));
            helper.setFrom(MailFrom, MailFromName);
            helper.setReplyTo(MailFrom, MailFromName);
            helper.setTo(recipients);
            helper.setText(getText(alertPolicy, dataSink), true);
            mailSender.send(message);
        }catch (Exception e) {
            logger.error("metric=eagle-mailNotify||alertPolicy={}||error=", alertPolicy, e);
        }
    }
}
