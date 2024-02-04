package com.alarm.eagle.api.dao.repository;

import com.alarm.eagle.api.dao.PolicyDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by luxiaoxun on 18/1/3.
 */
@Repository
public interface PolicyRepository extends JpaRepository<PolicyDo, Integer> {
    List<PolicyDo> queryByTaskId(Integer taskId);

    List<PolicyDo> queryByTaskIdAndStatus(Integer taskId, Integer status);

    PolicyDo queryFirstByPolicyId(Integer policyId);
}
