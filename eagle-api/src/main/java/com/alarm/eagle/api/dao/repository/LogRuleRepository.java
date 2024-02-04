package com.alarm.eagle.api.dao.repository;

import com.alarm.eagle.api.dao.LogRuleDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by luxiaoxun on 2020/01/29.
 */
@Repository
public interface LogRuleRepository extends JpaRepository<LogRuleDo, Long> {
}
