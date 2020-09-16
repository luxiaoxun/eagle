package com.alarm.eagle.policy.service;

import com.alarm.eagle.bean.DataSink;
import com.alarm.eagle.bean.Task;
import org.junit.*;
import org.junit.Test;

import java.util.Date;

/**
 * Created by skycrab on 18/1/5.
 */
public class ApiServiceTest {

    @Test
    public void testTaskApi() {
        Integer taskId = 1;
        Task task = ApiService.getInstance().queryTask(taskId);
        //System.out.println(task1);
        Assert.assertEquals(taskId, task.getTaskId());
    }

    @Test
    public void testSinkApi() {
        DataSink dataSink = new DataSink();
        dataSink.setTaskId(1);
        dataSink.setCreateTime(new Date());
        dataSink.setAppId(1);
        dataSink.setPolicyId(1);
        dataSink.setMetric("metric_test");
        dataSink.setData("testsink");
        ApiService.getInstance().sink(dataSink);
    }
}
