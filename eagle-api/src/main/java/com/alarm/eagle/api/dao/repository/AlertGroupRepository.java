package com.alarm.eagle.api.dao.repository;

import com.alarm.eagle.api.dao.AlertGroupDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by luxiaoxun on 18/1/16.
 */
@Repository
public interface AlertGroupRepository extends JpaRepository<AlertGroupDo, Integer> {
    AlertGroupDo getOne(Integer groupId);
}
