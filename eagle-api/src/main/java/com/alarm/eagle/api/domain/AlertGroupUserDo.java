package com.alarm.eagle.api.domain;

import com.alarm.eagle.util.JsonUtil;

import javax.persistence.*;
import java.util.Date;

/**
 * 预警告警组
 */
@Table(name = "eagle_alert_group_user")
@Entity
public class AlertGroupUserDo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, name = "id")
    private Integer id;

    @Column(nullable = false, name = "group_id")
    private Integer groupId;

    @Column(nullable = false, name = "user_name")
    private String userName;

    /**
     * 状态,0：停用，1：启用
     */
    @Column(nullable = false, name = "status")
    private Integer status;

    @Column(nullable = false, name = "create_time")
    private Date createTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
