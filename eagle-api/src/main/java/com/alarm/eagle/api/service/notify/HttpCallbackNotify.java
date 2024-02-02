package com.alarm.eagle.api.service.notify;

import com.alarm.eagle.api.util.HttpUtil;
import com.alarm.eagle.model.AlertPolicy;
import com.alarm.eagle.model.DataSink;
import com.alarm.eagle.constants.AlertConstant.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Created by luxiaoxun on 18/1/17.
 */
@Component
public class HttpCallbackNotify implements Notify {

    private static final Logger logger = LoggerFactory.getLogger(HttpCallbackNotify.class);

    @Override
    public AlertType getAlertType() {
        return AlertType.HttpCallback;
    }

    @Override
    public void notify(AlertPolicy alertPolicy, DataSink dataSink) {
        String result = HttpUtil.postBody(alertPolicy.getCallback(), dataSink.getData());
        logger.info("metric=eagle-callbackFailed||url={}||result={}||data={}", alertPolicy.getCallback(), result, dataSink.getData());
    }
}
