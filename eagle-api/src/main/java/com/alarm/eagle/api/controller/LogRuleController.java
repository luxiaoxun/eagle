package com.alarm.eagle.api.controller;

import com.alarm.eagle.api.bean.LogRule;
import com.alarm.eagle.api.service.LogRuleService;
import com.alarm.eagle.response.Response;
import com.alarm.eagle.response.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by luxiaoxun on 2020/01/29.
 */
@RestController
@RequestMapping("/log")
public class LogRuleController {
    private static final Logger logger = LoggerFactory.getLogger(LogRuleController.class);

    @Resource
    private LogRuleService logRuleService;

    @RequestMapping("/trace")
    public Response receive(String data) {
        logger.info("data={}", data);
        return ResponseUtil.success();
    }

    @RequestMapping("/rules")
    public Response rule() {
        List<LogRule> logRuleList = logRuleService.queryAllRules();
        return ResponseUtil.success(logRuleList);
    }

}
