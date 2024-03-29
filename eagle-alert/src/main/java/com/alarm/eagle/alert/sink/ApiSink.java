package com.alarm.eagle.alert.sink;

import com.alarm.eagle.model.DataSink;
import com.alarm.eagle.model.Task;
import com.alarm.eagle.alert.service.ApiService;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by skycrab on 18/1/9.
 */
public class ApiSink implements SinkFunction<DataSink> {
    private static final Logger logger = LoggerFactory.getLogger(ApiSink.class);

    private Task task;

    public ApiSink(Task task) {
        this.task = task;
    }

    @Override
    public void invoke(DataSink dataSink, Context context) throws Exception {
        logger.info("metric=eagle-apiSink||dataSink={}", dataSink);
        ApiService.getInstance().sink(dataSink);
    }
}
