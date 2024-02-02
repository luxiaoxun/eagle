package com.alarm.eagle.api.controller;

import com.alarm.eagle.api.dao.DataSinkDo;
import com.alarm.eagle.api.dao.repository.DataSinkRepository;
import com.alarm.eagle.api.service.NotifyService;
import com.alarm.eagle.model.DataSink;
import com.alarm.eagle.response.Response;
import com.alarm.eagle.response.ResponseUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;

/**
 * Created by luxiaoxun on 18/1/4.
 */
@RestController
@RequestMapping("/data")
@Api(tags = "data")
public class DataController {
    private static final Logger logger = LoggerFactory.getLogger(DataController.class);

    @Resource
    private DataSinkRepository dataSinkRepository;

    @Resource
    private NotifyService notifyService;

    @ApiOperation(value = "保存策略结果", notes = "保存策略结果")
    @PostMapping("/sink")
    public Response sink(@RequestBody DataSinkDo sinkDo) {
        logger.info("sinkDo={}", sinkDo);
        dataSinkRepository.save(sinkDo);
        DataSink dataSink = new DataSink();
        dataSink.setData(sinkDo.getData());
        dataSink.setCreateTime(sinkDo.getCreateTime());
        dataSink.setMetric(sinkDo.getMetric());
        dataSink.setPolicyId(sinkDo.getPolicyId());
        dataSink.setTaskId(sinkDo.getTaskId());
        dataSink.setAppId(sinkDo.getAppId());
        notifyService.notify(dataSink);
        return ResponseUtil.success();
    }

    @PostMapping("/test")
    public Response test() {
        DataSinkDo sinkDo = new DataSinkDo();
        sinkDo.setAppId(1);
        sinkDo.setCreateTime(new Date());
        sinkDo.setData("data");
        sinkDo.setMetric("hello");
        sinkDo.setPolicyId(1);
        sinkDo.setTaskId(1);
        dataSinkRepository.save(sinkDo);
        return ResponseUtil.success();
    }
}
