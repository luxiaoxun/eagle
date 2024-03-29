package com.alarm.eagle.api.dao.repository;

import com.alarm.eagle.api.dao.StreamAppDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by luxiaoxun on 18/1/3.
 */
@Repository
public interface StreamAppRepository extends JpaRepository<StreamAppDo, Integer> {
    List<StreamAppDo> queryByTaskId(Integer taskId);

    List<StreamAppDo> queryByTaskIdAndStatus(Integer taskId, Integer status);
}
