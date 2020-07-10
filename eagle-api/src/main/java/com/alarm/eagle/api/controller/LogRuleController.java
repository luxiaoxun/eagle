package com.alarm.eagle.api.controller;

import com.alarm.eagle.api.bean.LogRule;
import com.alarm.eagle.api.domain.LogRuleDo;
import com.alarm.eagle.api.service.LogRuleService;
import com.alarm.eagle.response.Response;
import com.alarm.eagle.response.ResponseUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * Created by luxiaoxun on 2020/01/29.
 */
@RestController
@RequestMapping("/log")
@Api(tags = "log")
public class LogRuleController {
    private static final Logger logger = LoggerFactory.getLogger(LogRuleController.class);

    @Autowired
    private LogRuleService logRuleService;

    @ApiOperation(value = "查询所有的规则", notes = "查询所有的规则")
    @GetMapping("/rules")
    public Response getRules() {
        List<LogRule> logRuleList = logRuleService.queryAllRules();
        return ResponseUtil.success(logRuleList);
    }

    @ApiOperation(value = "查询规则", notes = "查询规则")
    @GetMapping("/rule/{id}")
    public Response getRuleById(@PathVariable("id") long id) {
        LogRule logRule = logRuleService.queryRuleById(id);
        if (logRule != null) {
            return ResponseUtil.success(logRule);
        } else {
            return ResponseUtil.fail(404, "没找到对应的规则");
        }
    }

    @ApiOperation(value = "保存或更新规则", notes = "保存或更新规则")
    @PostMapping("/rule")
    public Response saveOrUpdateRules(@RequestBody LogRuleDo logRuleDo) {
        logger.info("logRuleDo={}", logRuleDo);
        logRuleDo.setUpdateTime(new Date());
        LogRuleDo ret = logRuleService.saveOrUpdateRule(logRuleDo);
        return ResponseUtil.success(ret);
    }

    @ApiOperation(value = "删除规则", notes = "删除规则")
    @DeleteMapping("/rule/{id}")
    public Response deleteRule(@PathVariable("id") long id) {
        logger.info("deleteRuleById={}", id);
        boolean ret = logRuleService.deleteRuleById(id);
        if (ret) {
            return ResponseUtil.success(id);
        } else {
            return ResponseUtil.fail(500, "删除规则失败");
        }
    }

}
