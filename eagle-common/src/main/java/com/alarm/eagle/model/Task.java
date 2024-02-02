package com.alarm.eagle.model;

import com.alarm.eagle.constants.AlertConstant.*;
import com.alarm.eagle.util.JsonUtil;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.io.Serializable;
import java.util.List;

/**
 * Created by luxiaoxun on 17/12/25.
 */
public class Task implements Serializable{
    /**
     * 任务id
     */
    private Integer taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * flink time类型
     */
    private TimeCharacteristic timeCharacteristic;

    /**
     * 数据源列表，一个task多个source(多个队列)
     */
    private List<Datasource> datasourceList;

    /**
     * app列表
     */
    private List<StreamApp> streamAppList;

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

    public TimeCharacteristic getTimeCharacteristic() {
        return timeCharacteristic;
    }

    public void setTimeCharacteristic(TimeCharacteristic timeCharacteristic) {
        this.timeCharacteristic = timeCharacteristic;
    }

    public List<Datasource> getDatasourceList() {
        return datasourceList;
    }

    public void setDatasourceList(List<Datasource> datasourceList) {
        this.datasourceList = datasourceList;
    }

    public List<StreamApp> getStreamAppList() {
        return streamAppList;
    }

    public void setStreamAppList(List<StreamApp> streamAppList) {
        this.streamAppList = streamAppList;
    }

    @Override
    public String toString() {
        return JsonUtil.encode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    /**
     * app是否有修改
     * @return
     */
    public boolean streamAppModify(Task task) {
        return !JsonUtil.encode(this.getStreamAppList()).equals(JsonUtil.encode(task.getStreamAppList()));
    }

}
