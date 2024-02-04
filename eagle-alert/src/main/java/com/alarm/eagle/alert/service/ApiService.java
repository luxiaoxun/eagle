package com.alarm.eagle.alert.service;

import com.alarm.eagle.model.DataSink;
import com.alarm.eagle.model.Task;
import com.alarm.eagle.alert.config.EagleProperties;
import com.alarm.eagle.alert.constant.PropertiesConstant;
import com.fasterxml.jackson.core.type.TypeReference;
import com.alarm.eagle.util.JsonUtil;
import com.alarm.eagle.util.HttpUtil;
import com.alarm.eagle.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by skycrab on 18/1/5.
 */
public class ApiService {

    private static final String API_BASE_UEL = EagleProperties.getInstance().getProperty(PropertiesConstant.API_BASE_UEL_NAME);
    private static final String TASK_QUERY_URL = "/task/query";
    private static final String DATA_SINK_URL = "/data/sink";

    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);

    private static class ApiServiceHolder {
        private static final ApiService apiServiceHolder = new ApiService();
    }

    private ApiService() {

    }

    public static ApiService getInstance() {
        return ApiServiceHolder.apiServiceHolder;
    }

    public Task queryTask(Integer taskId) {
        Map<String, String> params = new HashMap<>();
        params.put("taskId", taskId.toString());
        String url = API_BASE_UEL + TASK_QUERY_URL;
        String result = HttpUtil.doPost(url, params);
        logger.info("metric=eagle-queryTask||url={}||params={}||result={}", url, params, result);
        try {
            Response<Task> taskResponse = JsonUtil.decode(result, new TypeReference<Response<Task>>() {
            });
            if (taskResponse != null && taskResponse.getCode() == 0) {
                return taskResponse.getData();
            }
        } catch (Exception e) {
            logger.error("metric=eagle-queryTask||url={}||error=", url, e);
        }

        return null;
    }

    public void sink(DataSink dataSink) {
        String url = API_BASE_UEL + DATA_SINK_URL;
        String body = JsonUtil.encode(dataSink);
        String result = HttpUtil.doPostJson(url, body);
        logger.info("metric=eagle-sink||url={}||dataSink={}||result={}", url, body, result);
    }
}
