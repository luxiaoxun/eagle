package com.alarm.eagle.api.bootstrap;

import com.alarm.eagle.api.service.EsIndexManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class Server implements InitializingBean, DisposableBean {
    @Autowired
    private EsIndexManager esIndexManager;

    @Override
    public void afterPropertiesSet() throws Exception {
    }

    @Override
    public void destroy() throws Exception {
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onAppStarted() {
        esIndexManager.initIndexAndSetting();
    }
}
