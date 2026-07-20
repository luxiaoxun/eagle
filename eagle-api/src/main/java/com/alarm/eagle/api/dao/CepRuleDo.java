package com.alarm.eagle.api.dao;

import com.alarm.eagle.util.JsonUtil;

import javax.persistence.*;
import java.util.Date;

/**
 * Eagle Dynamic CEP 规则 DO
 */
@Table(name = "eagle_cep_rule",
       uniqueConstraints = @UniqueConstraint(columnNames = {"rule_id", "version"}))
@Entity
public class CepRuleDo {
    private static final long serialVersionUID = 1L;

    /** 自增主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, name = "id")
    private Long id;

    /** 规则唯一标识 */
    @Column(nullable = false, name = "rule_id")
    private String ruleId;

    /** 规则版本号 */
    @Column(nullable = false, name = "version")
    private Integer version;

    @Column(nullable = false, name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(nullable = false, name = "pattern")
    private String pattern;

    @Column(name = "function")
    private String function;

    @Column(nullable = false, name = "status")
    private Integer status;

    @Column(nullable = false, name = "create_time")
    private Date createTime;

    @Column(nullable = false, name = "update_time")
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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
