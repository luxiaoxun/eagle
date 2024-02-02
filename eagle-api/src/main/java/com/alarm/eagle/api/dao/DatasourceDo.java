package com.alarm.eagle.api.dao;

import com.alarm.eagle.util.JsonUtil;

import javax.persistence.*;
import java.util.Date;

/**
 * 预警任务数据源配置
 * Created by luxiaoxun on 18/1/3.
 */
@Table(name = "eagle_datasource")
@Entity
public class DatasourceDo {
    /**
     * 数据源id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, name = "datasource_id")
    private Integer datasourceId;

    /**
     * 任务id
     */
    @Column(nullable = false, name = "task_id")
    private Integer taskId;

    /**
     * 队列topic
     */
    @Column(nullable = false, name = "topic")
    private String topic;

    /**
     * 队列地址
     */
    @Column(nullable = false, name = "servers")
    private String servers;

    /**
     * 队列groupId
     */
    @Column(nullable = false, name = "group_id")
    private String groupId;

    /**
     * 对应siddhi streamId
     */
    @Column(nullable = false, name = "stream_id")
    private String streamId;

    /**
     * 时间戳字段，用于flink watermark
     */
    @Column(nullable = false, name = "event_timestamp_field")
    private String eventTimestampField;

    /**
     * flink keyBy分组字段(,分割)
     */
    @Column(nullable = false, name = "key_by_fields")
    private String keyByFields;

    /**
     * 过滤表达式
     */
    @Column(nullable = false, name = "filter")
    private String filter;

    /**
     * 状态 0停用 1启用
     */
    @Column(nullable = false, name = "status")
    private Integer status;

    /**
     * 创建日期
     */
    @Column(nullable = false, name = "create_time")
    private Date createTime;

    public Integer getDatasourceId() {
        return datasourceId;
    }

    public void setDatasourceId(Integer datasourceId) {
        this.datasourceId = datasourceId;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

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

    public String getEventTimestampField() {
        return eventTimestampField;
    }

    public void setEventTimestampField(String eventTimestampField) {
        this.eventTimestampField = eventTimestampField;
    }

    public String getKeyByFields() {
        return keyByFields;
    }

    public void setKeyByFields(String keyByFields) {
        this.keyByFields = keyByFields;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
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
