package com.alarm.eagle.api.controller;

import com.alarm.eagle.api.service.mock.DataMockService;
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
    private DataMockService dataMockService;

    @Operation(summary = "发送mock数据")
    @PostMapping("/data")
    public Response sendEventMockData(@RequestParam(defaultValue = "1") Integer threads,
                                      @RequestParam(defaultValue = "10") Integer count) {
        log.info(String.format("%s threads try to send %s mock data", threads, count));
        dataMockService.sendEventMockData(threads, count);
        return ResponseUtil.success();
    }

    @Operation(summary = "发送json mock数据")
    @PostMapping("/data/json")
    public Response sendMockData(@RequestBody String jsonData) {
        dataMockService.sendEventMockData(jsonData);
        return ResponseUtil.success();
    }

}
