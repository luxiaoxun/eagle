package com.alarm.eagle.api.bean;

import com.alarm.eagle.constants.AlertConstant.Status;
import com.alarm.eagle.util.JsonUtil;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.io.Serializable;
import java.util.List;

/**
 * siddhi配置
 * Created by luxiaoxun on 17/12/25.
 */
public class StreamApp implements Serializable {
    /**
     * appId
     */
    private Integer appId;

    /**
     * app显示名称
     */
    private String appName;

    /**
     * 状态，是否启用
     */
    private Status status;

    /**
     * siddhi stream define
     */
    private List<StreamDefine> streamDefineList;

    /**
     * 查询列表
     */
    private List<Policy> policyList;

    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<Policy> getPolicyList() {
        return policyList;
    }

    public void setPolicyList(List<Policy> policyList) {
        this.policyList = policyList;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<StreamDefine> getStreamDefineList() {
        return streamDefineList;
    }

    public void setStreamDefineList(List<StreamDefine> streamDefineList) {
        this.streamDefineList = streamDefineList;
    }

    @Override
    public String toString() {
        return JsonUtil.encode(this);
    }

    public String getSiddhiAppName() {
        return "app_"+appId;
    }

    public String toDefinition() {
        StringBuilder builder = new StringBuilder("@App:name('");
        builder.append(getSiddhiAppName());
        builder.append("')");
        builder.append("\n");
        for(StreamDefine streamDefine : getStreamDefineList()) {
            builder.append("define stream ");
            builder.append(streamDefine.getStreamId());
            builder.append(" (");
            int i = 0;
            int end = streamDefine.getFieldList().size()-1;
            for(Field field : streamDefine.getFieldList()) {
                builder.append(field.getFieldName());
                builder.append(" ");
                builder.append(field.getFieldType());
                if(i < end ) {
                    builder.append(",");
                }
                i++;
            }
            builder.append(");");
            builder.append("\n");
        }

        return builder.toString();
    }

    public String toQueryPlan() {
        StringBuilder builder = new StringBuilder(toDefinition());
        for(Policy policy : getPolicyList()) {
            builder.append(policy.toQuery());
            builder.append("\n");
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
