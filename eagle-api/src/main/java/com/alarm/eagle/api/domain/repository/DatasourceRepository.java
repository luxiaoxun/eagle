package com.alarm.eagle.api.domain.repository;

import com.alarm.eagle.api.domain.DatasourceDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by luxiaoxun on 18/1/3.
 */
public interface DatasourceRepository extends JpaRepository<DatasourceDo, Integer> {
    List<DatasourceDo> queryByTaskId(Integer taskId);
}
