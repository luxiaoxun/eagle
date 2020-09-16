package com.alarm.eagle.api.controller;

import com.alarm.eagle.api.service.TaskService;
import com.alarm.eagle.bean.Task;
import com.alarm.eagle.response.Response;
import com.alarm.eagle.response.ResponseUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Created by luxiaoxun on 18/1/2.
 */
@RestController
@RequestMapping("/task")
@Api(tags = "task")
public class TaskController {
    @Resource
    public TaskService taskService;

    @ApiOperation(value = "查询任务", notes = "查询任务")
    @GetMapping("/query")
    public Response queryTask(@RequestParam Integer taskId) {
        Task task = taskService.queryTask(taskId);
        return ResponseUtil.success(task);
    }
}
