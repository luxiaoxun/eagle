package com.alarm.eagle.api.service.notify;

import com.alarm.eagle.bean.AlertPolicy;
import com.alarm.eagle.bean.DataSink;
import com.alarm.eagle.constants.AlertConstant.*;

/**
 * Created by luxiaoxun on 18/1/17.
 */
public interface Notify {
    AlertType getAlertType();

    /**
     * 通知
     * @param alertPolicy
     */
    void notify(AlertPolicy alertPolicy, DataSink dataSink);
}
