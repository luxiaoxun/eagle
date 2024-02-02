package com.alarm.eagle.api.dao.repository;

import com.alarm.eagle.api.dao.AlertGroupDo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by luxiaoxun on 18/1/16.
 */
public interface AlertGroupRepository extends JpaRepository<AlertGroupDo, Integer> {
    AlertGroupDo getOne(Integer groupId);
}
