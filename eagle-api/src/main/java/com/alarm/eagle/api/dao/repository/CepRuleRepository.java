package com.alarm.eagle.api.dao.repository;

import com.alarm.eagle.api.dao.CepRuleDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Eagle Dynamic CEP 规则 Repository
 */
@Repository
public interface CepRuleRepository extends JpaRepository<CepRuleDo, Long> {

    /** 查询所有 ACTIVE 状态的规则（按 rule_id 分组，取每组的最大版本） */
    @Query("SELECT c FROM CepRuleDo c WHERE c.status = 1 " +
           "AND c.version = (SELECT MAX(c2.version) FROM CepRuleDo c2 " +
           "WHERE c2.ruleId = c.ruleId AND c2.status = 1) ORDER BY c.ruleId")
    List<CepRuleDo> findAllActive();

    /** 按 ruleId 和 status 查询 */
    List<CepRuleDo> findByRuleIdAndStatus(String ruleId, Integer status);

    /** 查询指定 ruleId 最新版本且为 ACTIVE 的规则 */
    @Query("SELECT c FROM CepRuleDo c WHERE c.ruleId = :ruleId AND c.status = 1 " +
           "ORDER BY c.version DESC")
    List<CepRuleDo> findLatestByRuleId(@Param("ruleId") String ruleId);
}
