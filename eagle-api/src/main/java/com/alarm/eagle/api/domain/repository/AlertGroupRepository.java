package com.alarm.eagle.api.domain.repository;

import com.alarm.eagle.api.domain.AlertGroupDo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by luxiaoxun on 18/1/16.
 */
public interface AlertGroupRepository extends JpaRepository<AlertGroupDo, Integer> {
    AlertGroupDo getOne(Integer groupId);
}
