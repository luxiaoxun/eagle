package com.alarm.eagle.bean;

import com.alarm.eagle.util.JsonUtil;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by luxiaoxun on 2020/01/29.
 */
public class LogRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * app id
     */
    private String appId;

    /**
     * 版本
     */
    private String version;

    /**
     * 解析类型
     */
    private String type;

    /**
     * 解析脚本
     */
    private String script;

    /**
     * 状态 1有效 0无效
     */
    private Integer state;

    /**
     * 最后更新时间
     */
    private Date updateTime;

    public LogRule() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
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