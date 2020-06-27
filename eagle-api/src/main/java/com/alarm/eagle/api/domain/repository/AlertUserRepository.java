package com.alarm.eagle.api.domain.repository;

import com.alarm.eagle.api.domain.AlertUserDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by luxiaoxun on 18/1/16.
 */
public interface AlertUserRepository extends JpaRepository<AlertUserDo, Integer> {
    List<AlertUserDo> queryByUserNameIn(List<String> userNameList);
}
