package com.alarm.eagle.api.domain.repository;

import com.alarm.eagle.api.domain.StreamDefineDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by luxiaoxun on 18/1/3.
 */
public interface StreamDefineRepository extends JpaRepository<StreamDefineDo, Integer> {

    List<StreamDefineDo> queryByTaskId(Integer taskId);
}
