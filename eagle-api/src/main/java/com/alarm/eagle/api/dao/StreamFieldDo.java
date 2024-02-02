package com.alarm.eagle.api.dao;

import com.alarm.eagle.util.JsonUtil;

import javax.persistence.*;
import java.util.Date;

/**
 * 预警任务流属性字段配置
 * Created by luxiaoxun on 18/1/3.
 */
@Table(name = "eagle_app_stream_field")
@Entity
public class StreamFieldDo {
    /**
     * 自增id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, name = "field_id")
    private Integer fieldId;

    /**
     * 对应AppStreamDefine Id
     */
    @Column(nullable = false, name = "stream_define_id")
    private Integer stream_define_id;

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
     * 字段名称
     */
    @Column(nullable = false, name = "field_name")
    private String fieldName;

    /**
     * 字段类型
     */
    @Column(nullable = false, name = "field_type")
    private String fieldType;

    /**
     * 创建时间
     */
    @Column(nullable = false, name = "create_time")
    private Date createTime;

    public Integer getFieldId() {
        return fieldId;
    }

    public void setFieldId(Integer fieldId) {
        this.fieldId = fieldId;
    }

    public Integer getStream_define_id() {
        return stream_define_id;
    }

    public void setStream_define_id(Integer stream_define_id) {
        this.stream_define_id = stream_define_id;
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

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
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
