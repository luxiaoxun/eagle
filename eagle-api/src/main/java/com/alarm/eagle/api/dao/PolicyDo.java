package com.alarm.eagle.api.dao;

import com.alarm.eagle.util.JsonUtil;

import javax.persistence.*;
import java.util.Date;

/**
 * 预警任务策略配置
 * Created by luxiaoxun on 18/1/3.
 */
@Table(name = "eagle_app_policy")
@Entity
public class PolicyDo {
    /**
     * 策略id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, name = "policy_id")
    private Integer policyId;

    /**
     * 策略名称
     */
    @Column(nullable = false, name = "policy_name")
    private String policyName;

    /**
     * 策略聚合指标,类tag功能
     */
    @Column(nullable = false, name = "metric")
    private String metric;

    /**
     * 任务id
     */
    @Column(nullable = false, name = "task_id")
    private Integer taskId;

    /**
     * 应用id
     */
    @Column(nullable = false, name = "app_id")
    private Integer appId;

    /**
     * 预定义流结构
     */
    @Column(nullable = false, name = "prepare_stream")
    private String prepareStream;

    /**
     * 策略cql
     */
    @Column(nullable = false, name = "cql")
    private String cql;

    /**
     * 报警级别,1恢复 2警告 3紧急 4严重
     */
    @Column(nullable = false, name = "alert_level")
    private Integer alertLevel;

    /**
     * 状态,0：停用，1：启用
     */
    @Column(nullable = false, name = "status")
    private Integer status;

    /**
     * 创建时间
     */
    @Column(nullable = false, name = "create_time")
    private Date createTime;

    public Integer getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Integer policyId) {
        this.policyId = policyId;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getPrepareStream() {
        return prepareStream;
    }

    public void setPrepareStream(String prepareStream) {
        this.prepareStream = prepareStream;
    }

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

    public String getCql() {
        return cql;
    }

    public void setCql(String cql) {
        this.cql = cql;
    }

    public Integer getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(Integer alertLevel) {
        this.alertLevel = alertLevel;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
