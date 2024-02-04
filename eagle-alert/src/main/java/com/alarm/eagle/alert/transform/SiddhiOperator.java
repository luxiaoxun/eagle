package com.alarm.eagle.alert.transform;

import com.alarm.eagle.model.DataSink;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.alarm.eagle.util.DateUtil;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.runtime.state.StateInitializationContext;
import org.apache.flink.runtime.state.StateSnapshotContext;
import org.apache.flink.streaming.api.graph.StreamConfig;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.operators.Output;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.runtime.tasks.StreamTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Created by skycrab on 17/12/25.
 */
public class SiddhiOperator extends AbstractStreamOperator<DataSink> implements OneInputStreamOperator<Tuple2<String, ObjectNode>, DataSink> {

    private static final Logger logger = LoggerFactory.getLogger(SiddhiOperator.class);
    private static final int QUEUE_INITIAL_CAPACITY = 16;
    private static final String SIDDHI_STATE_NAME = "EagleSiddhiListState";
    private static final String QUEUE_STATE_NAME = "EagleQueueListState";

    /**
     * 任务id
     */
    private int taskId;

    /**
     * 是否外部数据处理时间
     */
    private final boolean isEventTime;

    private transient SiddhiTaskManager siddhiTaskManager;

    private transient PriorityQueue<StreamRecord<Tuple2<String, ObjectNode>>> priorityQueue;

    private transient ListState<Tuple2<String, byte[]>> siddhiRuntimeState;

    private transient ListState<StreamRecord<Tuple2<String, ObjectNode>>> queueState;

    public SiddhiOperator(int taskId, boolean isEventTime) {
        this.taskId = taskId;
        this.isEventTime = isEventTime;
    }

    @Override
    public void processElement(StreamRecord<Tuple2<String, ObjectNode>> streamRecord) throws Exception {
        logger.info("metric=eagle-processElement||streamRecord={}", streamRecord);
        String streamId = streamRecord.getValue().f0;
        ObjectNode node = streamRecord.getValue().f1;
        if (isEventTime) {
            priorityQueue.offer(streamRecord);
        } else {
            siddhiTaskManager.send(System.currentTimeMillis(), streamId, node);
        }
    }

    @Override
    public void processWatermark(Watermark mark) throws Exception {
        logger.info("metric=eagle-processWatermark||watermark={}||watermarkStr={}", mark.getTimestamp(), DateUtil.fromUnixtime(mark.getTimestamp()));
        while (!priorityQueue.isEmpty() && priorityQueue.peek().getTimestamp() <= mark.getTimestamp()) {
            StreamRecord<Tuple2<String, ObjectNode>> streamRecord = priorityQueue.poll();
            siddhiTaskManager.send(streamRecord.getTimestamp(), streamRecord.getValue().f0, streamRecord.getValue().f1);
        }
        output.emitWatermark(mark);
    }

    @Override
    public void setup(StreamTask<?, ?> containingTask, StreamConfig config, Output<StreamRecord<DataSink>> output) {
        super.setup(containingTask, config, output);
        priorityQueue = new PriorityQueue<>(QUEUE_INITIAL_CAPACITY, new StreamRecordComparator());
        siddhiTaskManager = new SiddhiTaskManager(taskId, output);
        siddhiTaskManager.setUp();
    }

    @Override
    public void snapshotState(StateSnapshotContext context) throws Exception {
        super.snapshotState(context);
        checkpointSiddhiRuntimeState();
        checkpointQueueState();
    }

    /**
     * 参考 http://flink.iteblog.com/dev/stream/state.html#using-managed-operator-state
     */
    @Override
    public void initializeState(StateInitializationContext context) throws Exception {
        super.initializeState(context);

        //siddhiRuntime使用Union redistribution（联合重分布）
        ListStateDescriptor<Tuple2<String, byte[]>> siddhiDescriptor =
                new ListStateDescriptor<>(SIDDHI_STATE_NAME, TypeInformation.of(new TypeHint<Tuple2<String, byte[]>>() {
                }));
        siddhiRuntimeState = context.getOperatorStateStore().getUnionListState(siddhiDescriptor);

        //快照数据可重新分发到其它算子,使用Even-split redistribution（均分重分布）
        ListStateDescriptor<StreamRecord<Tuple2<String, ObjectNode>>> queueDescriptor =
                new ListStateDescriptor<>(QUEUE_STATE_NAME, TypeInformation.of(new TypeHint<StreamRecord<Tuple2<String, ObjectNode>>>() {
                }));
        queueState = context.getOperatorStateStore().getListState(queueDescriptor);

        //发生错误做了恢复,应用快照
        if (context.isRestored()) {
            restoreSiddhiRuntime();
            restoreQueue();
        }
    }


    @Override
    public void close() throws Exception {
        siddhiTaskManager.dispose();
        super.close();
    }

    /**
     * 保存siddhi运行时快照
     */
    private void checkpointSiddhiRuntimeState() throws Exception {
        siddhiRuntimeState.clear();
        for (Map.Entry<String, byte[]> entry : siddhiTaskManager.snapshot().entrySet()) {
            siddhiRuntimeState.add(Tuple2.of(entry.getKey(), entry.getValue()));
        }
    }

    /**
     * 快照恢复siddhi运行时
     */
    private void restoreSiddhiRuntime() throws Exception {
        Map<String, byte[]> snapshotMap = new HashMap<>();
        for (Tuple2<String, byte[]> tuple2 : siddhiRuntimeState.get()) {
            snapshotMap.put(tuple2.f0, tuple2.f1);
        }
        siddhiTaskManager.restore(snapshotMap);
    }

    /**
     * 保存优先级队列快照
     */
    private void checkpointQueueState() throws Exception {
        queueState.clear();
        for (StreamRecord<Tuple2<String, ObjectNode>> record : priorityQueue) {
            queueState.add(record);
        }
    }

    /**
     * 快照恢复优先级队列
     */
    private void restoreQueue() throws Exception {
        for (StreamRecord<Tuple2<String, ObjectNode>> record : queueState.get()) {
            priorityQueue.offer(record);
        }
    }
}
