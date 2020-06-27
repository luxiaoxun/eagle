package com.alarm.eagle.api.domain.repository;

import com.alarm.eagle.api.domain.PolicyDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by luxiaoxun on 18/1/3.
 */
public interface PolicyRepository extends JpaRepository<PolicyDo, Integer> {
    List<PolicyDo> queryByTaskId(Integer taskId);
    List<PolicyDo> queryByTaskIdAndStatus(Integer taskId, Integer status);
    PolicyDo queryFirstByPolicyId(Integer policyId);
}
