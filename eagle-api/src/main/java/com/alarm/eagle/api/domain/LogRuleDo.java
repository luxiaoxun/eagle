package com.alarm.eagle.api.domain;

import com.alarm.eagle.util.JsonUtil;
import javax.persistence.*;
import java.util.Date;

/**
 * Created by luxiaoxun on 2020/01/29.
 */
@Table(name = "log_rule")
@Entity
public class LogRuleDo {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, name = "id")
    private Long id;

    /**
     * app id
     */
    @Column(nullable = false, name = "app_id")
    private String appId;

    /**
     * 版本
     */
    @Column(nullable = false, name = "version")
    private String version;

    /**
     * 解析类型
     */
    @Column(nullable = false, name = "type")
    private String type;

    /**
     * 解析脚本
     */
    @Column(nullable = false, name = "script")
    private String script;

    /**
     * 状态 1有效 0无效
     */
    @Column(nullable = false, name = "state")
    private Integer state;

    /**
     * 最后更新时间
     */
    @Column(nullable = false, name = "update_time")
    private Date updateTime;

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
}
