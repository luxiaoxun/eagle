package com.alarm.eagle.api.domain;

import com.alarm.eagle.util.JsonUtil;

import javax.persistence.*;
import java.util.Date;

/**
 * 预警任务应用配置
 * Created by luxiaoxun on 18/1/3.
 */
@Table(name = "eagle_app")
@Entity
public class StreamAppDo {
    /**
     * 应用id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, name = "app_id")
    private Integer appId;

    /**
     * 应用名称
     */
    @Column(nullable = false, name = "app_name")
    private String appName;

    /**
     * 任务id
     */
    @Column(nullable = false, name = "task_id")
    private Integer taskId;

    /**
     * 状态 0停用 1启用
     */
    @Column(nullable = false, name = "status")
    private Integer status;

    /**
     * 创建时间
     */
    @Column(nullable = false, name = "create_time")
    private Date createTime;

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

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
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
