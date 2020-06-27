package com.alarm.eagle.api.domain.repository;

import com.alarm.eagle.api.domain.StreamAppDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by luxiaoxun on 18/1/3.
 */
public interface StreamAppRepository extends JpaRepository<StreamAppDo, Integer> {
    List<StreamAppDo> queryByTaskId(Integer taskId);
    List<StreamAppDo> queryByTaskIdAndStatus(Integer taskId, Integer status);
}
