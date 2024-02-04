package com.alarm.eagle.api.dao.repository;

import com.alarm.eagle.api.dao.DataSinkDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by luxiaoxun on 18/1/4.
 */
@Repository
public interface DataSinkRepository extends JpaRepository<DataSinkDo, Long> {
}
