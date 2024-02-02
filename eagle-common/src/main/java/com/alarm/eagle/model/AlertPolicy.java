package com.alarm.eagle.model;

import com.alarm.eagle.constants.AlertConstant.*;
import com.alarm.eagle.util.JsonUtil;

import java.util.Date;
import java.util.List;

/**
 * Created by luxiaoxun on 18/1/16.
 */
public class AlertPolicy {
    private Integer policyId;

    private Policy policy;

    /**
     * 告警方式
     */
    private List<AlertType> alertTypeList;

    /**
     * cd时间
     */
    private Integer cd;

    private Status status;

    /**
     * 文本模板
     */
    private String txtTemplate;

    /**
     * html模板
     */
    private String htmlTemplate;

    /**
     * 回调地址
     */
    private String callback;

    /**
     * 组id
     */
    private Integer groupId;

    /**
     * 组名称
     */
    private String groupName;

    /**
     * 组用户
     */
    private List<AlertUser> userList;

    /**
     * 上一次通知时间
     */
    private Date lastAlertTime;

    private Date createTime;

    public Integer getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Integer policyId) {
        this.policyId = policyId;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public List<AlertType> getAlertTypeList() {
        return alertTypeList;
    }

    public void setAlertTypeList(List<AlertType> alertTypeList) {
        this.alertTypeList = alertTypeList;
    }

    public Integer getCd() {
        return cd;
    }

    public void setCd(Integer cd) {
        this.cd = cd;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<AlertUser> getUserList() {
        return userList;
    }

    public void setUserList(List<AlertUser> userList) {
        this.userList = userList;
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
