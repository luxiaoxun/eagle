package com.alarm.eagle.alert;

import com.alarm.eagle.model.DataSink;
import com.alarm.eagle.model.Task;
import com.alarm.eagle.alert.config.EagleProperties;
import com.alarm.eagle.alert.transform.SiddhiOperator;
import com.alarm.eagle.alert.transform.SiddhiUnionStream;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.alarm.eagle.alert.service.ApiService;
import com.alarm.eagle.alert.sink.ApiSink;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alarm.eagle.constants.AlertConstant.TimeCharacteristic.EventTime;


/**
 * 启动程序
 */
public class EagleAlertApp {
    private static final Logger logger = LoggerFactory.getLogger(EagleAlertApp.class);

    public static void main(String[] args) throws Exception {
        logger.info("metric=eagle||args={}", args);
        Integer taskId;
        boolean devMode = EagleProperties.getInstance().isDevMode();
        if (args.length < 1) {
            if (devMode) {
                taskId = 1;
            } else {
                throw new IllegalArgumentException("missing taskId");
            }
        } else {
            taskId = Integer.valueOf(args[0]);
        }

        Task task = ApiService.getInstance().queryTask(taskId);
        if (task == null) {
            logger.error("task init error");
            throw new RuntimeException("task init error");
        }


        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        Integer optParallelism = EagleProperties.getInstance().getParallelism();
        if (optParallelism != null) {
            env.setParallelism(optParallelism);
        }

        switch (task.getTimeCharacteristic()) {
            case ProcessingTime:
                env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);
            case IngestionTime:
                env.setStreamTimeCharacteristic(TimeCharacteristic.IngestionTime);
            case EventTime:
                env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        }

        //checkpoint
        if (!devMode && EagleProperties.getInstance().enableCheckPoint()) {
            env.enableCheckpointing(60000L);
            CheckpointConfig config = env.getCheckpointConfig();
            config.setMinPauseBetweenCheckpoints(30000L);
            config.setCheckpointTimeout(8000L);
            config.enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        }

        SiddhiUnionStream siddhiStream = new SiddhiUnionStream(task, env);
        DataStream<Tuple2<String, ObjectNode>> unionDatastream = siddhiStream.transform();

        boolean isEventTime = task.getTimeCharacteristic() == EventTime;
        TypeInformation<DataSink> typeInfo = TypeExtractor.createTypeInfo(DataSink.class);
        DataStream<DataSink> alertDatastream = unionDatastream.transform("siddhi", typeInfo,
                new SiddhiOperator(taskId, isEventTime));
        alertDatastream.addSink(new ApiSink(task));

        env.execute();
    }
}
