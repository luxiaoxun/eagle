package com.alarm.eagle.api.dao.repository;

import com.alarm.eagle.api.dao.AlertGroupUserDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by luxiaoxun on 18/1/16.
 */
public interface AlertGroupUserRepository extends JpaRepository<AlertGroupUserDo, Integer>{
    List<AlertGroupUserDo> queryByGroupIdAndStatus(Integer groupId, Integer status);
    List<AlertGroupUserDo> queryByGroupId(Integer groupId);
}
