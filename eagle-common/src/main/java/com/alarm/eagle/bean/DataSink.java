package com.alarm.eagle.bean;

import com.alarm.eagle.util.JsonUtil;

import java.util.Date;

public class DataSink {

    /**
     * 任务id
     */
    private Integer taskId;

    /**
     * 应用id
     */
    private Integer appId;

    /**
     * 策略聚合指标,类tag功能
     */
    private String metric;

    /**
     * 策略id
     */
    private Integer policyId;

    /**
     * 命中结果
     */
    private String data;

    private Date createTime;

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public Integer getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Integer policyId) {
        this.policyId = policyId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return JsonUtil.encode(this);
    }
}
