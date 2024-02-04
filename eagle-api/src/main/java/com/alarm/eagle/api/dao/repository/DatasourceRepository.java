package com.alarm.eagle.api.dao.repository;

import com.alarm.eagle.api.dao.DatasourceDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by luxiaoxun on 18/1/3.
 */
@Repository
public interface DatasourceRepository extends JpaRepository<DatasourceDo, Integer> {
    List<DatasourceDo> queryByTaskId(Integer taskId);
}
