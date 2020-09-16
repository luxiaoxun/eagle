package com.alarm.eagle.policy.siddihistrem;

import com.alarm.eagle.bean.Field;
import com.alarm.eagle.bean.StreamApp;
import com.alarm.eagle.bean.StreamDefine;
import com.alarm.eagle.bean.Task;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.alarm.eagle.util.JsonUtil;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.graph.StreamConfig;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.operators.Output;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.runtime.tasks.StreamTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.input.InputHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by skycrab on 17/12/25.
 */
public class SiddhiObjectNodeTestOperator extends AbstractStreamOperator<ObjectNode> implements OneInputStreamOperator<Tuple2<String, ObjectNode>, ObjectNode> {

    private static final Logger logger = LoggerFactory.getLogger(SiddhiObjectNodeTestOperator.class);

    private transient SiddhiManager siddhiManager;

    private transient ConcurrentHashMap<String, InputHandler> inputHandlers;

    private Task task;

    public SiddhiObjectNodeTestOperator(Task task) {
        this.task = task;
    }

    @Override
    public void processElement(StreamRecord<Tuple2<String, ObjectNode>> streamRecord) throws Exception {
        String streamId = streamRecord.getValue().f0;
        ObjectNode node = streamRecord.getValue().f1;
        for (StreamApp streamApp : task.getStreamAppList()) {
            for (StreamDefine streamDefine : streamApp.getStreamDefineList()) {
                if (streamDefine.getStreamId().equals(streamId)) {
                    Object[] data = nodeToArray(streamDefine, node);
                    if (streamId.equals("stream1")) {
                        getInputHandler(streamId).send(data);
                    }
                }
            }
        }
    }

    /**
     * ObjectNode转为siddhi需要的Object
     *
     * @param streamDefine
     * @return
     */
    private Object[] nodeToArray(StreamDefine streamDefine, ObjectNode node) {
        Object[] data = new Object[streamDefine.getFieldList().size()];
        List<Field> fieldList = streamDefine.getFieldList();

        for (int i = 0; i < fieldList.size(); i++) {
            Field field = fieldList.get(i);
            String key = field.getFieldName();
            Object value = null;
            switch (field.getFieldType()) {
                case "string":
                    value = node.get(key).asText();
                    break;
                case "int":
                    value = node.get(key).asInt();
                    break;
                case "long":
                    value = node.get(key).asLong();
                    break;
                case "double":
                    value = node.get(key).asDouble();
                    break;
                case "boolean":
                    value = node.get(key).asBoolean();
                    break;
            }
            data[i] = value;
        }
        return data;
    }

    @Override
    public void setup(StreamTask<?, ?> containingTask, StreamConfig config, Output<StreamRecord<ObjectNode>> output) {
        super.setup(containingTask, config, output);
        inputHandlers = new ConcurrentHashMap<>();
        createAppRuntime();
    }

    /**
     * 注册streamId对应的inputHandler, streamId唯一
     *
     * @param streamId
     * @param inputHandler
     */
    private void registerInputHandler(String streamId, InputHandler inputHandler) {
        inputHandlers.put(streamId, inputHandler);
    }

    /**
     * 获取注册的inputHandler
     *
     * @param streamId
     * @return
     */
    private InputHandler getInputHandler(String streamId) {
        return inputHandlers.get(streamId);
    }

    private void createAppRuntime() {
        siddhiManager = new SiddhiManager();
        String siddhiApp = "" +
                "@App:name('Test')" +
                "define stream stream1 (timestamp string, temperature double, rack int); " +
                "@info(name = 'query') " +
                "from stream1[temperature>40] " +
                "select timestamp,temperature,rack " +
                ",time:extract('hour', '2014-3-11 02:23:44', 'yyyy-MM-dd hh:mm:ss') as hour " +
                "insert into outputStream ;";
        //Generating runtime
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        //Adding callback to retrieve output events from query
        siddhiAppRuntime.addCallback("query", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                StreamRecord<ObjectNode> reusableRecord = new StreamRecord<>(null, 0L);
                //EventPrinter.print(timestamp, inEvents, removeEvents);
                for (Event event : inEvents) {
                    Map<String, Object> row = new HashMap<>(3);
                    row.put("timestamp", event.getData(0));
                    row.put("temperature", event.getData(1));
                    row.put("rack", event.getData(2));
                    row.put("hour", event.getData(3));
                    ObjectNode objectNode = JsonUtil.convertValue(row, ObjectNode.class);
                    logger.info("policy receive:{}", objectNode);
                    reusableRecord.replace(objectNode, event.getTimestamp());
                    output.collect(reusableRecord);
                }
            }
        });
        String streamId = "stream1";
        registerInputHandler(streamId, siddhiAppRuntime.getInputHandler(streamId));

        //Starting event processing
        siddhiAppRuntime.start();

//      //Shutting down the runtime
//      siddhiAppRuntime.shutdown();
//
//      //Shutting down Siddhi
//      siddhiManager.shutdown();

    }
}
