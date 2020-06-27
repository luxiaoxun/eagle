package com.alarm.eagle.api.service;

import com.alarm.eagle.api.bean.LogRule;
import com.alarm.eagle.api.domain.LogRuleDo;
import com.alarm.eagle.api.domain.repository.LogRuleRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luxiaoxun on 2020/01/29.
 */
@Service
public class LogRuleService {
    @Resource
    private LogRuleRepository logRuleRepository;

    public List<LogRule> queryAllRules() {
        List<LogRuleDo> logRuleDoList = logRuleRepository.findAll();
        List<LogRule> results = new ArrayList<>(logRuleDoList.size());
        for (LogRuleDo logRuleDo : logRuleDoList) {
            LogRule logRule = new LogRule(logRuleDo);
            results.add(logRule);
        }
        return results;
    }
}
