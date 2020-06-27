package com.alarm.eagle.api.bean;

import com.alarm.eagle.constants.AlertConstant.Status;
import com.alarm.eagle.constants.AlertConstant.AlertLevel;
import com.alarm.eagle.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.io.Serializable;

/**
 * Created by luxiaoxun on 17/12/25.
 */
public class Policy implements Serializable{
    /**
     * 策略id
     */
    private Integer policyId;

    /**
     * 策略可读名称
     */
    private String policyName;

    /**
     * 策略聚合指标,类tag功能
     */
    private String metric;

    /**
     * 预定义流结构
     */
    private String prepareStream;

    /**
     * 策略语句cql
     */
    private String cql;

    /**
     * 是否启用
     */
    private Status status;

    /**
     * 告警级别
     */
    private AlertLevel alertLevel;

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

    public String getCql() {
        return cql;
    }

    public void setCql(String cql) {
        this.cql = cql;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public AlertLevel getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(AlertLevel alertLevel) {
        this.alertLevel = alertLevel;
    }

    @Override
    public String toString() {
        return JsonUtil.encode(this);
    }

    public String getQueryName() {
        return "policy_"+policyId;
    }

    public String getOutputName() {
        return "output_"+policyId;
    }

    public String toQuery() {
        StringBuilder builder = new StringBuilder();
        String prepare = getPrepareStream().trim();
        if(StringUtils.isNotBlank(prepare)) {
            builder.append(prepare);
            if(!prepare.endsWith(";")) {
                builder.append(";");
            }
            builder.append("\n");
        }
        builder.append("@info(name = '");
        builder.append(getQueryName());
        builder.append("')");
        builder.append("\n");
        builder.append(getCql().trim());
        builder.append("\n");
        builder.append("insert into ");
        builder.append(getOutputName());
        builder.append(";");
        return builder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
