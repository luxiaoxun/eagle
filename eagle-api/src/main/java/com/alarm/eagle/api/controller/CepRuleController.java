package com.alarm.eagle.api.controller;

import com.alarm.eagle.api.dao.CepRuleDo;
import com.alarm.eagle.api.service.CepRuleService;
import com.alarm.eagle.model.CepRule;
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
 * Eagle Dynamic CEP 规则管理 Controller
 *
 * <p>提供给 eagle-detection Flink job 拉取 CEP 规则：
 * <ul>
 *   <li>GET  /cep/rules      — 获取所有 ACTIVE 规则（eagle-detection 使用）</li>
 *   <li>GET  /cep/rules/hash — 获取规则 hash（用于变更检测）</li>
 *   <li>GET  /cep/rule/{id}  — 获取单个规则</li>
 *   <li>POST /cep/rule       — 创建/更新规则</li>
 *   <li>PUT  /cep/rule/{id}/status — 更新规则状态</li>
 *   <li>DELETE /cep/rule/{id} — 删除规则</li>
 * </ul>
 */
@RestController
@RequestMapping("/cep")
@Api(tags = "cep")
public class CepRuleController {
    private static final Logger logger = LoggerFactory.getLogger(CepRuleController.class);

    @Autowired
    private CepRuleService cepRuleService;

    @ApiOperation(value = "查询所有 ACTIVE CEP 规则", notes = "eagle-detection 用此接口拉取规则")
    @GetMapping("/rules")
    public Response getActiveRules() {
        List<CepRule> rules = cepRuleService.queryAllActiveRules();
        return ResponseUtil.success(rules);
    }

    @ApiOperation(value = "查询所有 CEP 规则（含非 ACTIVE）", notes = "管理后台使用")
    @GetMapping("/rules/all")
    public Response getAllRules() {
        List<CepRule> rules = cepRuleService.queryAllRules();
        return ResponseUtil.success(rules);
    }

    @ApiOperation(value = "获取规则 hash", notes = "eagle-detection 用此接口检测规则变更")
    @GetMapping("/rules/hash")
    public Response getRulesHash() {
        String hash = cepRuleService.getRulesHash();
        return ResponseUtil.success(hash);
    }

    @ApiOperation(value = "查询单个 CEP 规则", notes = "按 ruleId 查询最新版本规则")
    @GetMapping("/rule/{ruleId}")
    public Response getRuleByRuleId(@PathVariable("ruleId") String ruleId) {
        CepRule rule = cepRuleService.queryRuleByRuleId(ruleId);
        if (rule != null) {
            return ResponseUtil.success(rule);
        } else {
            return ResponseUtil.fail("没找到对应的 CEP 规则: " + ruleId);
        }
    }

    @ApiOperation(value = "创建或更新 CEP 规则", notes = "保存规则，同 ruleId 的版本号自动递增")
    @PostMapping("/rule")
    public Response saveOrUpdateRule(@RequestBody CepRuleDo cepRuleDo) {
        logger.info("saveOrUpdateCepRule: {}", cepRuleDo);
        cepRuleDo.setUpdateTime(new Date());
        CepRuleDo ret = cepRuleService.saveOrUpdateRule(cepRuleDo);
        return ResponseUtil.success(ret);
    }

    @ApiOperation(value = "更新 CEP 规则状态", notes = "status: 1=ACTIVE, 0=PAUSED, -1=DELETED")
    @PutMapping("/rule/{ruleId}/status")
    public Response updateStatus(@PathVariable("ruleId") String ruleId, @RequestParam Integer status) {
        logger.info("updateCepRuleStatus: ruleId={}, status={}", ruleId, status);
        boolean ret = cepRuleService.updateStatus(ruleId, status);
        if (ret) {
            return ResponseUtil.success("ok");
        } else {
            return ResponseUtil.fail("更新规则状态失败: " + ruleId);
        }
    }

    @ApiOperation(value = "删除 CEP 规则", notes = "软删除：status 设为 -1")
    @DeleteMapping("/rule/{ruleId}")
    public Response deleteRule(@PathVariable("ruleId") String ruleId) {
        logger.info("deleteCepRule: ruleId={}", ruleId);
        boolean ret = cepRuleService.deleteRule(ruleId);
        if (ret) {
            return ResponseUtil.success(ruleId);
        } else {
            return ResponseUtil.fail("删除规则失败: " + ruleId);
        }
    }
}
