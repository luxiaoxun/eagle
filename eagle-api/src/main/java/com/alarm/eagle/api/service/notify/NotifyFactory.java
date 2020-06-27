package com.alarm.eagle.api.service.notify;

import com.alarm.eagle.constants.AlertConstant.*;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by luxiaoxun on 18/1/17.
 */
@Component
public class NotifyFactory implements ApplicationContextAware{
    private Map<AlertType, Notify> alertNotify = new HashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Notify> notifyBean = applicationContext.getBeansOfType(Notify.class);
        for(Notify notify : notifyBean.values()) {
            alertNotify.put(notify.getAlertType(), notify);
        }
    }

    public Notify getNotify(AlertType alertType) {
        return alertNotify.get(alertType);
    }

    public List<Notify> getNotify(List<AlertType> alertTypeList) {
        return alertTypeList.stream().map(alertType -> getNotify(alertType)).collect(Collectors.toList());
    }
}
