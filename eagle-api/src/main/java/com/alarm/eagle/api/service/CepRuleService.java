package com.alarm.eagle.api.service;

import com.alarm.eagle.api.dao.CepRuleDo;
import com.alarm.eagle.api.dao.repository.CepRuleRepository;
import com.alarm.eagle.model.CepRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Eagle Dynamic CEP 规则 Service
 */
@Service
public class CepRuleService {
    private static final Logger logger = LoggerFactory.getLogger(CepRuleService.class);

    @Resource
    private CepRuleRepository cepRuleRepository;

    /** 查询所有 ACTIVE 状态的规则（用于 eagle-detection 拉取规则） */
    public List<CepRule> queryAllActiveRules() {
        List<CepRuleDo> doList = cepRuleRepository.findAllActive();
        // 同一 ruleId 只保留最大 version 的那条
        Map<String, CepRuleDo> latestMap = new LinkedHashMap<>();
        for (CepRuleDo cepRuleDo : doList) {
            CepRuleDo existing = latestMap.get(cepRuleDo.getRuleId());
            if (existing == null || cepRuleDo.getVersion() > existing.getVersion()) {
                latestMap.put(cepRuleDo.getRuleId(), cepRuleDo);
            }
        }
        return latestMap.values().stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    /** 查询所有规则（包括非 ACTIVE 状态，用于管理后台） */
    public List<CepRule> queryAllRules() {
        List<CepRuleDo> doList = cepRuleRepository.findAll();
        return doList.stream().map(this::toModel).collect(Collectors.toList());
    }

    /** 按 ruleId 查询规则 */
    public CepRule queryRuleByRuleId(String ruleId) {
        List<CepRuleDo> doList = cepRuleRepository.findLatestByRuleId(ruleId);
        if (doList != null && !doList.isEmpty()) {
            return toModel(doList.get(0));
        }
        return null;
    }

    /** 保存或更新规则（版本号自动递增） */
    @Transactional
    public CepRuleDo saveOrUpdateRule(CepRuleDo cepRuleDo) {
        // 查询同 ruleId 的最大版本号
        List<CepRuleDo> existingList = cepRuleRepository.findLatestByRuleId(cepRuleDo.getRuleId());
        if (existingList != null && !existingList.isEmpty()) {
            // 已有同 ruleId 规则，版本号 +1
            cepRuleDo.setVersion(existingList.get(0).getVersion() + 1);
        } else {
            // 新规则，版本号从 1 开始
            cepRuleDo.setVersion(1);
        }
        cepRuleDo.setUpdateTime(new Date());
        if (cepRuleDo.getCreateTime() == null) {
            cepRuleDo.setCreateTime(new Date());
        }
        if (cepRuleDo.getStatus() == null) {
            cepRuleDo.setStatus(1);
        }
        return cepRuleRepository.saveAndFlush(cepRuleDo);
    }

    /** 更新规则状态 */
    @Transactional
    public boolean updateStatus(String ruleId, Integer status) {
        List<CepRuleDo> doList = cepRuleRepository.findLatestByRuleId(ruleId);
        if (doList != null && !doList.isEmpty()) {
            CepRuleDo cepRuleDo = doList.get(0);
            cepRuleDo.setStatus(status);
            cepRuleDo.setUpdateTime(new Date());
            cepRuleRepository.saveAndFlush(cepRuleDo);
            return true;
        }
        return false;
    }

    /** 删除规则（软删除：status = -1） */
    @Transactional
    public boolean deleteRule(String ruleId) {
        return updateStatus(ruleId, -1);
    }

    /** 获取规则 hash 值（用于变更检测） */
    public String getRulesHash() {
        List<CepRule> rules = queryAllActiveRules();
        String content = rules.stream()
                .map(r -> r.getRuleId() + ":" + r.getVersion())
                .sorted()
                .collect(Collectors.joining(","));
        return String.valueOf(content.hashCode());
    }

    private CepRule toModel(CepRuleDo cepRuleDo) {
        CepRule rule = new CepRule();
        rule.setRuleId(cepRuleDo.getRuleId());
        rule.setVersion(cepRuleDo.getVersion());
        rule.setName(cepRuleDo.getName());
        rule.setDescription(cepRuleDo.getDescription());
        rule.setPattern(cepRuleDo.getPattern());
        rule.setFunction(cepRuleDo.getFunction());
        rule.setStatus(cepRuleDo.getStatus());
        rule.setCreateTime(cepRuleDo.getCreateTime());
        rule.setUpdateTime(cepRuleDo.getUpdateTime());
        return rule;
    }
}
