package com.alarm.eagle.api.domain.repository;

import com.alarm.eagle.api.domain.StreamFieldDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by luxiaoxun on 18/1/3.
 */
public interface StreamFieldRepository extends JpaRepository<StreamFieldDo, Integer> {
    List<StreamFieldDo> queryByTaskId(Integer taskId);
}
