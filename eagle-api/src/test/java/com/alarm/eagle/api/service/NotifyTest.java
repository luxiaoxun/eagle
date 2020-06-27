package com.alarm.eagle.api.service;

import com.alarm.eagle.api.BootTestBase;
import com.alarm.eagle.api.util.TemplateEngine;
import com.alarm.eagle.api.bean.AlertPolicy;
import com.alarm.eagle.api.bean.AlertUser;
import com.alarm.eagle.api.bean.DataSink;
import com.alarm.eagle.api.bean.Policy;
import com.alarm.eagle.api.service.notify.*;
import com.alarm.eagle.constants.AlertConstant.*;

import com.alarm.eagle.util.JsonUtil;
import freemarker.template.Template;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by luxiaoxun on 18/1/17.
 */

public class NotifyTest extends BootTestBase {
    @Resource
    NotifyFactory notifyFactory;

    @Resource
    TemplateEngine engine;

    @Resource
    MailNotify mailNotify;

    @Resource
    SmsNotify smsNotify;

    @Resource
    WeixinNotify weixinNotify;

    @Test
    public void testNotify() {
        Notify notify = notifyFactory.getNotify(AlertType.Mail);
        Assert.assertEquals(notify.getAlertType(), AlertType.Mail);

        List<AlertType> alertTypeList = new ArrayList<>();
        alertTypeList.add(AlertType.Mail);
        alertTypeList.add(AlertType.SMS);

        List<Notify> notifyList = notifyFactory.getNotify(alertTypeList);
        Assert.assertEquals(notifyList.size(), 2);
    }

    @Test
    public void testTemplate() throws Exception {
        Map<String, Object> model = engine.getModel();
        model.put("name", "eagle");
        Template template = engine.getStringTemplate("hello ${name}");
        String result = engine.render(template, model);
        Assert.assertEquals(result, "hello eagle");
    }

    @Test
    public void testAlertNotify() {
        AlertPolicy alertPolicy = new AlertPolicy();
        //user
        AlertUser alertUser = new AlertUser();
        alertUser.setMail("luxiaoxun@163.com");
        alertUser.setPhone("111111111");
        alertUser.setWeixinName("luxiaoxun");
        List<AlertUser> alertUserList = new ArrayList<>();
        alertUserList.add(alertUser);
        alertPolicy.setUserList(alertUserList);
        //policy
        Policy policy = new Policy();
        policy.setAlertLevel(AlertLevel.CRITICAL);
        policy.setCql("select * from outputstream where a >10");
        policy.setPolicyName("测试");
        alertPolicy.setPolicy(policy);
        //
        DataSink dataSink = new DataSink();
        Map<String, Object> data = new HashMap<>();
        data.put("current", 100);
        dataSink.setData(JsonUtil.encode(data));

//        mailNotify.notify(alertPolicy, dataSink);
//        smsNotify.notify(alertPolicy, dataSink);
        weixinNotify.notify(alertPolicy, dataSink);
    }
}
