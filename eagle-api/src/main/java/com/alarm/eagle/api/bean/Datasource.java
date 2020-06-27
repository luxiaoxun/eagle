package com.alarm.eagle.api.bean;

import com.alarm.eagle.util.JsonUtil;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.io.Serializable;
import java.util.List;

/**
 * 数据源
 * Created by luxiaoxun on 17/12/25.
 */
public class Datasource implements Serializable{
    /**
     * 队列topic
     */
    private String topic;

    /**
     * 队列地址
     */
    private String servers;

    /**
     * 队列groupId
     */
    private String groupId;

    /**
     * siddhi stream
     */
    private String streamId;

    /**
     * 事件时间戳字段
     */
    private String eventTimestampField;

    /**
     * keyBy分组字段
     */
    private List<String> keyByFields;

    /**
     * 过滤表达式
     */
    private String filter;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getServers() {
        return servers;
    }

    public void setServers(String servers) {
        this.servers = servers;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public List<String> getKeyByFields() {
        return keyByFields;
    }

    public void setKeyByFields(List<String> keyByFields) {
        this.keyByFields = keyByFields;
    }

    public String getEventTimestampField() {
        return eventTimestampField;
    }

    public void setEventTimestampField(String eventTimestampField) {
        this.eventTimestampField = eventTimestampField;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    @Override
    public String toString() {
        return JsonUtil.encode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
