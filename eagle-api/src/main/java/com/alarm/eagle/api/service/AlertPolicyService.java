package com.alarm.eagle.api.service;

import com.alarm.eagle.api.domain.*;
import com.alarm.eagle.api.domain.repository.*;
import com.alarm.eagle.api.bean.AlertPolicy;
import com.alarm.eagle.api.bean.AlertUser;
import com.alarm.eagle.api.bean.Policy;
import com.alarm.eagle.constants.AlertConstant.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知
 * Created by luxiaoxun on 18/1/16.
 */
@Service
public class AlertPolicyService {
    @Resource
    private AlertGroupRepository alertGroupRepository;

    @Resource
    private AlertGroupUserRepository alertGroupUserRepository;

    @Resource
    private AlertPolicyRepository alertPolicyRepository;

    @Resource
    private AlertUserRepository alertUserRepository;

    @Resource
    private PolicyRepository policyRepository;

    /**
     * 获取策略id配置
     * @param policyId
     * @return
     */
    @Transactional
    public AlertPolicy queryPolicyAlert(Integer policyId) {
        AlertPolicyDo alertPolicyDo = alertPolicyRepository.queryFirstByPolicyIdAndStatus(policyId, Status.ENABLE.getId());
        if(alertPolicyDo == null) {
            return null;
        }
        Integer groupId = alertPolicyDo.getGroupId();
        //基础信息
        AlertPolicy alertPolicy = new AlertPolicy();
        alertPolicy.setPolicyId(alertPolicyDo.getPolicyId());
        alertPolicy.setAlertTypeList(AlertType.resolveAll(alertPolicyDo.getAlertType()));
        alertPolicy.setCallback(alertPolicyDo.getCallback());
        alertPolicy.setCd(alertPolicyDo.getCd());
        alertPolicy.setGroupId(groupId);
        alertPolicy.setHtmlTemplate(alertPolicyDo.getHtmlTemplate());
        alertPolicy.setTxtTemplate(alertPolicyDo.getTxtTemplate());
        alertPolicy.setLastAlertTime(alertPolicyDo.getLastAlertTime());
        alertPolicy.setStatus(Status.resolve(alertPolicyDo.getStatus()));
        alertPolicy.setCreateTime(alertPolicyDo.getCreateTime());

        //Policy
        PolicyDo policyDo = policyRepository.queryFirstByPolicyId(alertPolicyDo.getPolicyId());
        Policy policy = new Policy();
        policy.setPolicyId(policyDo.getPolicyId());
        policy.setPolicyName(policyDo.getPolicyName());
        policy.setMetric(policyDo.getMetric());
        policy.setPrepareStream(policyDo.getPrepareStream());
        policy.setCql(policyDo.getCql());
        policy.setStatus(Status.resolve(policyDo.getStatus()));
        policy.setAlertLevel(AlertLevel.resolve(policyDo.getAlertLevel()));
        alertPolicy.setPolicy(policy);

        //group User
        AlertGroupDo alertGroupDo = alertGroupRepository.getOne(groupId);
        alertPolicy.setGroupName(alertGroupDo.getGroupName());

        List<AlertGroupUserDo> alertGroupUserDoList = alertGroupUserRepository.queryByGroupIdAndStatus(groupId, Status.ENABLE.getId());
        List<String> userNameList = alertGroupUserDoList.stream().map(alertGroupUserDo -> alertGroupUserDo.getUserName()).collect(Collectors.toList());
        List<AlertUserDo> alertUserDoList = alertUserRepository.queryByUserNameIn(userNameList);
        List<AlertUser> alertUserList = new ArrayList<>(alertUserDoList.size());
        for(AlertUserDo  alertUserDo : alertUserDoList) {
            AlertUser alertUser = new AlertUser();
            alertUser.setUserName(alertUserDo.getUserName());
            alertUser.setMail(alertUserDo.getMail());
            alertUser.setPhone(alertUserDo.getPhone());
            alertUser.setWeixinName(alertUserDo.getWeixinName());
            alertUserList.add(alertUser);
        }
        alertPolicy.setUserList(alertUserList);

        return alertPolicy;
    }

    /**
     * 更新策略
     * 1.更新最后一次发送时间
     */
    @Transactional
    public void updatePolicyAlert(AlertPolicy alertPolicy) {
        alertPolicyRepository.updateByPolicyId(new Date(), alertPolicy.getPolicyId());
    }

}
