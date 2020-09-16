package com.alarm.eagle.bean;

import com.alarm.eagle.util.JsonUtil;

/**
 * Created by luxiaoxun on 18/1/16.
 */
public class AlertUser {
    /**
     * 账号名称
     */
    private String userName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String mail;

    /**
     * 微信用户名
     */
    private String weixinName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getWeixinName() {
        return weixinName;
    }

    public void setWeixinName(String weixinName) {
        this.weixinName = weixinName;
    }

    @Override
    public String toString() {
        return JsonUtil.encode(this);
    }
}
