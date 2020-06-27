package com.alarm.eagle.api.domain;

import com.alarm.eagle.util.JsonUtil;

import javax.persistence.*;
import java.util.Date;

/**
 * 告警组
 * Created by luxiaoxun on 18/1/16.
 */
@Table(name = "eagle_alert_group")
@Entity
public class AlertGroupDo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, name = "group_id")
    private Integer groupId;

    @Column(nullable = false, name = "group_name")
    private String groupName;

    @Column(nullable = false, name = "create_time")
    private Date createTime;

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