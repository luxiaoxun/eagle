package com.alarm.eagle.api.service;

import com.alarm.eagle.bean.LogRule;
import com.alarm.eagle.api.domain.LogRuleDo;
import com.alarm.eagle.api.domain.repository.LogRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by luxiaoxun on 2020/01/29.
 */
@Service
public class LogRuleService {
    private static final Logger logger = LoggerFactory.getLogger(LogRuleService.class);

    @Resource
    private LogRuleRepository logRuleRepository;

    public List<LogRule> queryAllRules() {
        List<LogRuleDo> logRuleDoList = logRuleRepository.findAll();
        List<LogRule> results = logRuleDoList.stream().map(e -> {
            return getLogRule(e);
        }).collect(Collectors.toList());
        return results;
    }

    public LogRule queryRuleById(long id) {
        Optional<LogRuleDo> logRuleDo = logRuleRepository.findById(id);
        if (logRuleDo.isPresent()) {
            return getLogRule(logRuleDo.get());
        }
        return null;
    }

    private LogRule getLogRule(LogRuleDo logRuleDo) {
        LogRule logRule = new LogRule();
        logRule.setId(logRuleDo.getId());
        logRule.setName(logRuleDo.getName());
        logRule.setAppId(logRuleDo.getAppId());
        logRule.setScript(logRuleDo.getScript());
        logRule.setType(logRuleDo.getType());
        logRule.setState(logRuleDo.getState());
        logRule.setVersion(logRuleDo.getVersion());
        logRule.setUpdateTime(logRuleDo.getUpdateTime());
        return logRule;
    }

    public LogRuleDo saveOrUpdateRule(LogRuleDo logRuleDo) {
        return logRuleRepository.saveAndFlush(logRuleDo);
    }

    public boolean deleteRuleById(long id) {
        boolean ret = false;
        try {
            logRuleRepository.deleteById(id);
            ret = true;
        } catch (Exception ex) {
            logger.error("Delete rule by id error:" + ex.toString());
        }
        return ret;
    }
}
