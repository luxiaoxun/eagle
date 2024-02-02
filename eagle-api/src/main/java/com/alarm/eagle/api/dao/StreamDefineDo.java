package com.alarm.eagle.api.dao;

import com.alarm.eagle.util.JsonUtil;

import javax.persistence.*;
import java.util.Date;

/**
 * 预警任务流属性配置
 * Created by luxiaoxun on 18/1/3.
 */
@Table(name = "eagle_app_stream_define")
@Entity
public class StreamDefineDo {
    /**
     * app stream 自增id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, name = "stream_define_id")
    private Integer streamDefineId;

    /**
     * 对应siddhi streamId
     */
    @Column(nullable = false, name = "stream_id")
    private String streamId;

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
     * 创建时间
     */
    @Column(nullable = false, name = "create_time")
    private Date createTime;

    public Integer getStreamDefineId() {
        return streamDefineId;
    }

    public void setStreamDefineId(Integer streamDefineId) {
        this.streamDefineId = streamDefineId;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
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
