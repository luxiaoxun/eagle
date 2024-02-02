package com.alarm.eagle.policy.transform;

import com.alarm.eagle.model.*;
import com.alarm.eagle.policy.constant.PropertiesConstant;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.alarm.eagle.policy.config.EagleProperties;
import com.alarm.eagle.policy.service.ApiService;
import com.alarm.eagle.util.DateUtil;
import org.apache.flink.streaming.api.operators.Output;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.util.persistence.InMemoryPersistenceStore;
import org.wso2.siddhi.query.api.definition.Attribute;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by skycrab on 18/1/8.
 */
public class SiddhiTaskManager {
    private static final Logger logger = LoggerFactory.getLogger(SiddhiTaskManager.class);

    private final int taskId;
    /**
     * 任务配置
     */
    private transient Task task;

    private transient Task prevTask;

    private transient SiddhiManager siddhiManager;

    private transient int TASK_WATCHER_INTERVAL;

    /**
     * 更新锁
     */
    private transient Object updateLock = new Object();
    private transient Map<String, InputHandler> inputHandlers;

    private transient Output<StreamRecord<DataSink>> output;

    public SiddhiTaskManager(int taskId, Output<StreamRecord<DataSink>> output) {
        this.taskId = taskId;
        this.output = output;
    }

    /**
     * @param timestamp  时间戳
     * @param streamId   流id标识符
     * @param objectNode 接受的数据
     */
    public void send(long timestamp, String streamId, ObjectNode objectNode) {
        logger.info("metric=eagle-siddhiSend||streamId={}||timestamp={}||objectNode={}", streamId, DateUtil.fromUnixtime(timestamp), objectNode);
        synchronized (updateLock) {
            for (StreamApp streamApp : task.getStreamAppList()) {
                for (StreamDefine streamDefine : streamApp.getStreamDefineList()) {
                    if (streamDefine.getStreamId().equals(streamId)) {
                        Object[] data = nodeToArray(streamDefine, objectNode);
                        try {
                            getInputHandler(streamDefine.getUniqueStreamId()).send(timestamp, data);
                            //getInputHandler(streamDefine.getUniqueStreamId()).send(data);
                        } catch (Exception e) {
                            logger.info("metric=eagle-siddhiSend||streamId={}||objectNode={}", streamId, objectNode);
                        }
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
    private Object[] nodeToArray(StreamDefine streamDefine, ObjectNode objectNode) {
        Object[] data = new Object[streamDefine.getFieldList().size()];
        List<Field> fieldList = streamDefine.getFieldList();

        for (int i = 0; i < fieldList.size(); i++) {
            Field field = fieldList.get(i);
            String key = field.getFieldName();
            Object value = null;
            if (objectNode.has(key)) {
                switch (field.getFieldType()) {
                    case "string":
                        value = objectNode.get(key).asText();
                        break;
                    case "int":
                        value = objectNode.get(key).asInt();
                        break;
                    case "long":
                        value = objectNode.get(key).asLong();
                        break;
                    case "double":
                        value = objectNode.get(key).asDouble();
                        break;
                    case "boolean":
                        value = objectNode.get(key).asBoolean();
                        break;
                }
            }
            data[i] = value;
        }
        return data;
    }

    /**
     * 注册uniqueStreamId对应的inputHandler
     *
     * @param uniqueStreamId
     * @param inputHandler
     */
    private void registerInputHandler(String uniqueStreamId, InputHandler inputHandler) {
        inputHandlers.put(uniqueStreamId, inputHandler);
    }

    /**
     * 取消注册uniqueStreamId对应的inputHandler
     *
     * @param uniqueStreamId
     */
    private void unRegisterInputHandler(String uniqueStreamId) {
        inputHandlers.remove(uniqueStreamId);
    }


    /**
     * 获取注册的inputHandler
     *
     * @param uniqueStreamId
     * @return
     */
    private InputHandler getInputHandler(String uniqueStreamId) {
        return inputHandlers.get(uniqueStreamId);
    }


    public void setUp() {
        siddhiManager = new SiddhiManager();
        siddhiManager.setPersistenceStore(new InMemoryPersistenceStore());
        inputHandlers = new HashMap<>();
        TASK_WATCHER_INTERVAL = Integer.valueOf(EagleProperties.getInstance().getProperty(PropertiesConstant.TASK_WATCHER_INTERVAL_NAME));
        taskInit();
        startTaskWatcher();
    }

    /**
     * 任务初始化
     */
    private void taskInit() {
        while (true) {
            task = ApiService.getInstance().queryTask(taskId);
            if (task != null) {
                task.getStreamAppList().forEach(streamApp -> newSiddhiAppRuntime(streamApp));
                break;
            }
        }
    }

    /**
     * 任务动态更新
     */
    private void taskUpdate() {
        Task curTask = ApiService.getInstance().queryTask(taskId);
        if (curTask == null) {
            return;
        }
        //任务变更
        boolean modify = task.streamAppModify(curTask);
        //logger.info("metric=eagle-taskCheck||appModify={}||task={}||curTask={}", modify, task, curTask);
        if (modify) {
            logger.info("metric=eagle-taskUpdate");
            synchronized (updateLock) {
                prevTask = task;
                task = curTask;
                doStreamAppDestory();
                doStreamAppModify();
            }
        }
    }

    /**
     * streamApp删除
     */
    private void doStreamAppDestory() {
        Set<Integer> streamAppIds = task.getStreamAppList().stream().map(streamApp -> streamApp.getAppId()).collect(Collectors.toSet());
        prevTask.getStreamAppList().stream().filter(streamApp -> !streamAppIds.contains(streamApp.getAppId())).forEach(streamApp -> streamAppDestory(streamApp));
    }

    /**
     * streamApp修改(包括增加)
     */
    private void doStreamAppModify() {
        Map<Integer, StreamApp> streamAppMap = prevTask.getStreamAppList().stream().collect(Collectors.toMap(StreamApp::getAppId, streamApp -> streamApp));
        task.getStreamAppList().stream().filter(streamApp -> !streamApp.equals(streamAppMap.get(streamApp.getAppId()))).forEach(streamApp -> streamAppUpdate(streamApp));
    }

    /**
     * 任务动态更新线程
     */
    private void startTaskWatcher() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                logger.info("metric=eagle-startTaskWatcher||taskId={}", taskId);
                while (true) {
                    try {
                        TimeUnit.SECONDS.sleep(TASK_WATCHER_INTERVAL);
                        taskUpdate();
                    } catch (Exception e) {
                        logger.error("metric=eagle-taskWatcher||error=", e);
                    }
                }
            }
        }).start();
    }

    /**
     * streamApp删除
     *
     * @param streamApp
     */
    private void streamAppDestory(StreamApp streamApp) {
        String siddhiAppName = streamApp.getSiddhiAppName();
        logger.info("metric=eagle-streamAppDestory||taskId={}||appName={}", taskId, siddhiAppName);
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.getSiddhiAppRuntime(siddhiAppName);
        for (StreamDefine streamDefine : streamApp.getStreamDefineList()) {
            unRegisterInputHandler(streamDefine.getUniqueStreamId());
        }
        siddhiAppRuntime.shutdown();
    }


    /**
     * StreamApp更新
     */
    private void streamAppUpdate(StreamApp streamApp) {
        logger.info("metric=eagle-streamAppUpdate||taskId={}||appName={}", taskId, streamApp.getSiddhiAppName());
        byte[] snapshot = snapshotAndDestorySiddhiAppRuntime(streamApp);
        SiddhiAppRuntime siddhiAppRuntime = newSiddhiAppRuntime(streamApp);
        restoreSiddhiAppRuntime(siddhiAppRuntime, snapshot);
    }

    /**
     * 获取app快照数据然后销毁
     *
     * @param streamApp
     * @return
     */
    private byte[] snapshotAndDestorySiddhiAppRuntime(StreamApp streamApp) {
        String siddhiAppName = streamApp.getSiddhiAppName();
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.getSiddhiAppRuntime(siddhiAppName);
        byte[] snapshot = null;
        if (siddhiAppRuntime != null) {
            logger.info("metric=eagle-snapshot||taskId={}||siddhiAppName={}", taskId, siddhiAppName);
            snapshot = siddhiAppRuntime.snapshot();
            siddhiAppRuntime.shutdown();
        }

        for (StreamDefine streamDefine : streamApp.getStreamDefineList()) {
            unRegisterInputHandler(streamDefine.getUniqueStreamId());
        }

        return snapshot;
    }

    /**
     * 创建新的siddhi运行时
     *
     * @param streamApp
     * @return
     */
    private SiddhiAppRuntime newSiddhiAppRuntime(StreamApp streamApp) {
        String appQueryPlan = streamApp.toQueryPlan();
        logger.info("metric=eagle-appNew||taskId={}||appQueryPlan={}", taskId, appQueryPlan);

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(appQueryPlan);

        //同一个app,streamId唯一
        //Retrieving InputHandler to push events into Siddhi
        for (StreamDefine streamDefine : streamApp.getStreamDefineList()) {
            registerInputHandler(streamDefine.getUniqueStreamId(), siddhiAppRuntime.getInputHandler(streamDefine.getStreamId()));
        }

        //Adding callback to retrieve output events from query
        for (Policy policy : streamApp.getPolicyList()) {
            DataSink dataSink = new DataSink();
            dataSink.setTaskId(taskId);
            dataSink.setAppId(streamApp.getAppId());
            dataSink.setMetric(policy.getMetric());
            dataSink.setPolicyId(policy.getPolicyId());
            dataSink.setCreateTime(new Date());
            List<Attribute> attributeList = siddhiAppRuntime.getStreamDefinitionMap().get(policy.getOutputName()).getAttributeList();
            CallbackCtx callbackCtx = new CallbackCtx(dataSink, attributeList, output);
            siddhiAppRuntime.addCallback(policy.getQueryName(), new PolicyQueryCallback(callbackCtx));
        }

        //Starting event processing
        siddhiAppRuntime.start();

        return siddhiAppRuntime;
    }


    /**
     * 恢复之前运行时数据
     *
     * @param siddhiAppRuntime
     * @param snapshot
     */
    private void restoreSiddhiAppRuntime(SiddhiAppRuntime siddhiAppRuntime, byte[] snapshot) {
        if (snapshot != null) {
            try {
                siddhiAppRuntime.restore(snapshot);
            } catch (Exception e) {
                logger.error("metric=eagle-restore||taskId={}||siddhiAppName={}||error=", taskId, siddhiAppRuntime.getName(), e);
            }
        }
    }

    /**
     * 销毁
     */
    public void dispose() {
        siddhiManager.shutdown();
    }

    /**
     * 获取运行时快照
     */
    public Map<String, byte[]> snapshot() {
        return siddhiManager.getSiddhiAppRuntimeMap().values().stream().collect(Collectors.toMap(SiddhiAppRuntime::getName, siddhiAppRuntime -> siddhiAppRuntime.snapshot()));
    }

    /**
     * 恢复运行时快照
     *
     * @param snapshotMap
     */
    public void restore(Map<String, byte[]> snapshotMap) {
        siddhiManager.getSiddhiAppRuntimeMap().values().forEach(siddhiAppRuntime -> restoreSiddhiAppRuntime(siddhiAppRuntime, snapshotMap.get(siddhiAppRuntime.getName())));
    }
}
