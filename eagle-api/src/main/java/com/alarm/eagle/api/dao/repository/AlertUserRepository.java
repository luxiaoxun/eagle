package com.alarm.eagle.api.dao.repository;

import com.alarm.eagle.api.dao.AlertUserDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by luxiaoxun on 18/1/16.
 */
@Repository
public interface AlertUserRepository extends JpaRepository<AlertUserDo, Integer> {
    List<AlertUserDo> queryByUserNameIn(List<String> userNameList);
}
