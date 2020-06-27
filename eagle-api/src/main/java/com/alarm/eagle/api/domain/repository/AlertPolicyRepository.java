package com.alarm.eagle.api.domain.repository;

import com.alarm.eagle.api.domain.AlertPolicyDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;

/**
 * Created by luxiaoxun on 18/1/16.
 */
public interface AlertPolicyRepository extends JpaRepository<AlertPolicyDo, Integer> {
    AlertPolicyDo queryFirstByPolicyId(Integer policyId);
    AlertPolicyDo queryFirstByPolicyIdAndStatus(Integer policyId, Integer status);
    @Modifying
    @Query(value = "update AlertPolicyDo set lastAlertTime = :lastAlertTime where policyId = :policyId")
    int updateByPolicyId(@Param("lastAlertTime") Date lastAlertTime, @Param("policyId") Integer policyId);
}
