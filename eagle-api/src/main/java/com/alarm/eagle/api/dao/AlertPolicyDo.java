package com.alarm.eagle.api.dao;

import com.alarm.eagle.util.JsonUtil;

import javax.persistence.*;
import java.util.Date;

/**
 * policy告警配置
 * Created by luxiaoxun on 18/1/16.
 */
@Table(name = "eagle_alert_policy")
@Entity
public class AlertPolicyDo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, name = "id")
    private Integer id;

    @Column(nullable = false, name = "policy_id")
    private Integer policyId;

    /**
     * 告警方式
     */
    @Column(nullable = false, name = "alert_type")
    private Integer AlertType;

    /**
     * cd时间
     */
    @Column(nullable = false, name = "cd")
    private Integer cd;

    /**
     * 状态,0：停用，1：启用
     */
    @Column(nullable = false, name = "status")
    private Integer status;

    /**
     * 报警组
     */
    @Column(nullable = false, name = "group_id")
    private Integer groupId;

    /**
     * 文本模板
     */
    @Column(nullable = false, name = "txt_template")
    private String txtTemplate;

    /**
     * html模板
     */
    @Column(nullable = false, name = "html_template")
    private String htmlTemplate;

    /**
     * 回调地址
     */
    @Column(nullable = false, name = "callback")
    private String callback;

    /**
     * 上一次发送地址
     */
    @Column(nullable = false, name = "last_alert_time")
    private Date lastAlertTime;

    @Column(nullable = false, name = "create_time")
    private Date createTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Integer policyId) {
        this.policyId = policyId;
    }

    public Integer getAlertType() {
        return AlertType;
    }

    public void setAlertType(Integer alertType) {
        AlertType = alertType;
    }

    public Integer getCd() {
        return cd;
    }

    public void setCd(Integer cd) {
        this.cd = cd;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public String getTxtTemplate() {
        return txtTemplate;
    }

    public void setTxtTemplate(String txtTemplate) {
        this.txtTemplate = txtTemplate;
    }

    public String getHtmlTemplate() {
        return htmlTemplate;
    }

    public void setHtmlTemplate(String htmlTemplate) {
        this.htmlTemplate = htmlTemplate;
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }

    public Date getLastAlertTime() {
        return lastAlertTime;
    }

    public void setLastAlertTime(Date lastAlertTime) {
        this.lastAlertTime = lastAlertTime;
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
