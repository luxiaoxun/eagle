package com.alarm.eagle.api.service;

import com.alarm.eagle.api.service.notify.Notify;
import com.alarm.eagle.api.service.notify.NotifyFactory;
import com.alarm.eagle.model.AlertPolicy;
import com.alarm.eagle.model.DataSink;
import com.alarm.eagle.util.DateUtil;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 异步通知服务
 * Created by luxiaoxun on 18/1/17.
 */
@Service
public class NotifyService {
    private static final Logger logger = LoggerFactory.getLogger(NotifyService.class);

    private static final int CorePoolSize = 2;

    private static final int MaxmumPoolSize = 4;

    private static final int KeepAliveTime = 10;

    private static final int QueueCapacity = 1000;

    private ThreadPoolExecutor executor;

    @Resource
    private NotifyFactory notifyFactory;

    @Resource
    private AlertPolicyService alertPolicyService;

    public NotifyService() {
        this.executor = new ThreadPoolExecutor(CorePoolSize, MaxmumPoolSize, KeepAliveTime, TimeUnit.MINUTES,
                new ArrayBlockingQueue<Runnable>(QueueCapacity), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public void notify(DataSink dataSink) {
        this.executor.execute(new NotifyTask(dataSink));
    }

    public class NotifyTask implements Runnable {
        private DataSink dataSink;
        public NotifyTask(DataSink dataSink) {
            this.dataSink = dataSink;
        }

        @Override
        public void run() {
            logger.info("metric=ealge-notify||dataSink={}", dataSink);
            try{
                AlertPolicy alertPolicy = alertPolicyService.queryPolicyAlert(dataSink.getPolicyId());
                if(alertPolicy == null) {
                    return;
                }
                //// TODO: 18/1/17 策略拆出去
                //cd
                long current = System.currentTimeMillis();
                long prev = DateUtil.unixTimestamp(alertPolicy.getLastAlertTime());
                if(alertPolicy.getCd() > 0 && (current-prev) <= alertPolicy.getCd()*1000) {
                    logger.info("metric=ealge-bingoCd||current={}||prev={}||dataSink={}", current, prev, dataSink);
                    return;
                }
                List<Notify> notifyList = notifyFactory.getNotify(alertPolicy.getAlertTypeList());
                for(Notify notify : notifyList) {
                    notify.notify(alertPolicy, dataSink);
                }
                alertPolicyService.updatePolicyAlert(alertPolicy);
            }catch (Exception e) {
                logger.error("metric=eagle-notifyService||dataSink={}", dataSink);
            }
        }
    }
}
