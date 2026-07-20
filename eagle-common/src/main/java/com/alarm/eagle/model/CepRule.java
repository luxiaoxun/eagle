package com.alarm.eagle.model;

import com.alarm.eagle.util.JsonUtil;

import java.io.Serializable;
import java.util.Date;

/**
 * Eagle Dynamic CEP 规则 Model（API 返回给 Flink job 使用）
 */
public class CepRule implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 规则唯一标识 */
    private String ruleId;
    /** 规则版本号 */
    private Integer version;
    /** 规则名称 */
    private String name;
    /** 规则描述 */
    private String description;
    /** Pattern JSON (GraphSpec 格式) */
    private String pattern;
    /** PatternProcessFunction 全限定类名 */
    private String function;
    /** 状态: 1=ACTIVE, 0=PAUSED, -1=DELETED */
    private Integer status;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public String getFunction() { return function; }
    public void setFunction(String function) { this.function = function; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return JsonUtil.encode(this);
    }
}
