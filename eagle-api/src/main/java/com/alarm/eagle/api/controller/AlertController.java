package com.alarm.eagle.api.controller;

import com.alarm.eagle.api.service.AlertPolicyService;
import com.alarm.eagle.response.Response;
import com.alarm.eagle.response.ResponseUtil;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Created by luxiaoxun on 18/1/16.
 */
@RestController
@RequestMapping("/alert")
@Api(tags = "alert")
public class AlertController {
    @Resource
    private AlertPolicyService alertService;

    @GetMapping("/query")
    public Response queryPolicyAlert(@RequestParam Integer policyId) {
        return ResponseUtil.success(alertService.queryPolicyAlert(policyId));
    }

}
