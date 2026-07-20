package com.alarm.eagle.api.controller;

import com.alarm.eagle.api.service.mock.LogDataMockService;
import com.alarm.eagle.api.service.mock.TransactionDataMockService;
import com.alarm.eagle.response.Response;
import com.alarm.eagle.response.ResponseUtil;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Api(tags = "mock")
@RequestMapping("/mock")
@Slf4j
@Profile("dev")
public class DataMockController {
    @Autowired
    private LogDataMockService logDataMockService;

    @Autowired
    private TransactionDataMockService transactionDataMockService;

    // ======================== Log Mock ========================

    @Operation(summary = "发送mock数据")
    @PostMapping("/data")
    public Response sendEventMockData(@RequestParam(defaultValue = "1") Integer threads,
                                      @RequestParam(defaultValue = "10") Integer count) {
        log.info(String.format("%s threads try to send %s mock data", threads, count));
        logDataMockService.sendEventMockData(threads, count);
        return ResponseUtil.success();
    }

    @Operation(summary = "发送json mock数据")
    @PostMapping("/data/json")
    public Response sendMockData(@RequestBody String jsonData) {
        logDataMockService.sendEventMockData(jsonData);
        return ResponseUtil.success();
    }

    // ======================== Transaction Mock ========================

    @Operation(summary = "发送单条随机交易数据")
    @PostMapping("/transaction")
    public Response sendSingleTransaction() {
        transactionDataMockService.sendSingleTransaction();
        return ResponseUtil.success();
    }

    @Operation(summary = "批量发送随机交易数据（用于压测和规则验证）")
    @PostMapping("/transaction/batch")
    public Response sendBatchTransactions(
            @RequestParam(defaultValue = "1") Integer threads,
            @RequestParam(defaultValue = "100") Integer count,
            @RequestParam(defaultValue = "100") Integer intervalMs,
            @RequestParam(defaultValue = "20") Integer largePercent) {
        log.info("{} threads sending {} transactions each, interval={}ms, largePercent={}%",
                threads, count, intervalMs, largePercent);
        transactionDataMockService.sendBatchTransactions(
                threads, count, intervalMs, largePercent);
        return ResponseUtil.success();
    }

    @Operation(summary = "发送小额+大额配对交易（触发 rule_small_then_large 规则）")
    @PostMapping("/transaction/small-then-large")
    public Response sendSmallThenLargePair(@RequestParam(defaultValue = "1") Long payerId) {
        transactionDataMockService.sendSmallThenLargePair(payerId);
        return ResponseUtil.success();
    }

}
