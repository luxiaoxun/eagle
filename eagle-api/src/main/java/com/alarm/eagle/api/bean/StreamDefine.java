package com.alarm.eagle.api.bean;

import com.alarm.eagle.util.JsonUtil;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.io.Serializable;
import java.util.List;

/**
 * siddhi stream define
 * Created by luxiaoxun on 17/12/26.
 */
public class StreamDefine implements Serializable {

    /**
     * 主键id
     */
    private Integer streamDefineId;

    /**
     * 流id
     */
    private String streamId;
    /**
     * 字段定义
     */
    private List<Field> fieldList;

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

    public List<Field> getFieldList() {
        return fieldList;
    }

    public void setFieldList(List<Field> fieldList) {
        this.fieldList = fieldList;
    }

    @Override
    public String toString() {
        return JsonUtil.encode(this);
    }

    /**
     * 获取内部streamId,唯一
     * @return
     */
    public String getUniqueStreamId() {
        return streamId+'_'+streamDefineId;
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
