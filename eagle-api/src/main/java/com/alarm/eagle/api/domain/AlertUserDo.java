package com.alarm.eagle.api.domain;

import com.alarm.eagle.util.JsonUtil;

import javax.persistence.*;

/**
 * 预警用户配置
 * Created by luxiaoxun on 18/1/16.
 */
@Table(name = "eagle_alert_user")
@Entity
public class AlertUserDo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, name = "id")
    private Integer id;

    /**
     * 账号名称
     */
    @Column(nullable = false, name = "user_name")
    private String userName;

    /**
     * 手机号
     */
    @Column(nullable = false, name = "phone")
    private String phone;

    /**
     * 邮箱
     */
    @Column(nullable = false, name = "mail")
    private String mail;

    /**
     * 微信用户名
     */
    @Column(nullable = false, name = "weixin_name")
    private String weixinName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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
