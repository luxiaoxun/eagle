package com.alarm.eagle.api.domain.repository;

import com.alarm.eagle.api.domain.LogRuleDo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by luxiaoxun on 2020/01/29.
 */
public interface LogRuleRepository extends JpaRepository<LogRuleDo, Integer> {
}
