package com.alarm.eagle.api.domain;

import com.alarm.eagle.util.JsonUtil;

import javax.persistence.*;
import java.util.Date;

/**
 * 预警任务表
 * Created by luxiaoxun on 18/1/2.
 */
@Table(name = "eagle_task")
@Entity
public class TaskDo {
    /**
     * 任务Id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, name="task_id")
    private Integer taskId;

    /**
     * 任务名称
     */
    @Column(nullable = false, name = "task_name")
    private String taskName;

    /**
     * flink time类型 0 ProcessingTime 1 IngestionTime 2 EventTime
     */
    @Column(nullable = false, name = "time_characteristic")
    private Integer timeCharacteristic;

    /**
     * 状态，0：停用，1：启用
     */
    @Column(nullable = false, name="status")
    private Integer status;

    /**
     * 注解
     */
    @Column(nullable = false, name="annotation")
    private String annotation;

    /**
     * 创建时间
     */
    @Column(nullable = false, name="create_time")
    private Date createTime;

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Integer getTimeCharacteristic() {
        return timeCharacteristic;
    }

    public void setTimeCharacteristic(Integer timeCharacteristic) {
        this.timeCharacteristic = timeCharacteristic;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
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
