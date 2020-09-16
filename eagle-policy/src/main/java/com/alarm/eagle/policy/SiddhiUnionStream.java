package com.alarm.eagle.policy;

import com.alarm.eagle.bean.Datasource;
import com.alarm.eagle.bean.Task;
import com.alarm.eagle.policy.constant.PropertiesConstant;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.alarm.eagle.policy.config.EagleProperties;
import com.alarm.eagle.policy.source.JsonStringSource;
import com.alarm.eagle.policy.transform.SiddhiFlatMap;
import com.alarm.eagle.util.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.alarm.eagle.constants.AlertConstant.TimeCharacteristic.EventTime;

/**
 * Created by skycrab on 18/1/5.
 */
public class SiddhiUnionStream {
    /**
     * 任务配置
     */
    Task task;
    StreamExecutionEnvironment env;

    private static final Logger logger = LoggerFactory.getLogger(SiddhiUnionStream.class);

    public SiddhiUnionStream(Task task, StreamExecutionEnvironment env) {
        this.task = task;
        this.env = env;
    }

    public DataStream<Tuple2<String, ObjectNode>> transform() {
        //构建带streamId的Datastream
        List<DataStream<Tuple2<String, ObjectNode>>> nameDatastreamList = new ArrayList<>();

        for (Datasource datasource : task.getDatasourceList()) {
            nameDatastreamList.add(addSource(datasource));
        }

        //将多个流合并
        DataStream<Tuple2<String, ObjectNode>> unionNameDatastrem = nameDatastreamList.get(0);
        for (int i = 1; i < nameDatastreamList.size(); i++) {
            unionNameDatastrem = unionNameDatastrem.union(nameDatastreamList.get(i));
        }

        return unionNameDatastrem;
    }

    private DataStream<Tuple2<String, ObjectNode>> addSource(Datasource datasource) {
        final String streamId = datasource.getStreamId();
        SourceFunction<String> source;
        if (EagleProperties.getInstance().isDevMode()) {
            source = new JsonStringSource(streamId);
        } else {
            Properties properties = new Properties();
            properties.setProperty(PropertiesConstant.SOURCE_BOOTSTRAP_SERVERS, datasource.getServers());
            properties.setProperty(PropertiesConstant.SOURCE_GROUP_ID, datasource.getGroupId());
            source = new FlinkKafkaConsumer010<>(datasource.getTopic(), new SimpleStringSchema(), properties);
        }

        DataStream<String> sourceStream = env.addSource(source);

        DataStream<ObjectNode> dataStream = sourceStream.flatMap(new SiddhiFlatMap(datasource.getFilter()));

        //处理watermarker
        if (task.getTimeCharacteristic() == EventTime &&
                StringUtils.isNotBlank((datasource.getEventTimestampField()))) {
            dataStream = processWaterMarker(dataStream, datasource.getEventTimestampField());
        }
        //处理keyBy
        if (!datasource.getKeyByFields().isEmpty()) {
            dataStream = processKeyByField(dataStream, datasource.getKeyByFields());
        } else {
            dataStream = dataStream.rebalance();
        }
        //构建streamId
        return dataStream.map(new MapFunction<ObjectNode, Tuple2<String, ObjectNode>>() {
            @Override
            public Tuple2<String, ObjectNode> map(ObjectNode jsonNodes) throws Exception {
                return Tuple2.of(streamId, jsonNodes);
            }
        });
    }

    /**
     * EventTime处理watermarker
     *
     * @param dataStream
     * @param timestampField
     */
    private DataStream<ObjectNode> processWaterMarker(DataStream<ObjectNode> dataStream, final String timestampField) {
        //增加unix时间戳
        dataStream = dataStream.map(new MapFunction<ObjectNode, ObjectNode>() {
            @Override
            public ObjectNode map(ObjectNode objectNode) throws Exception {
                objectNode.put("unix_timestamp", toUnixTimestamp(objectNode, timestampField));
                return objectNode;
            }
        });
        return dataStream.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<ObjectNode>(Time.minutes(1L)) {
            @Override
            public long extractTimestamp(ObjectNode jsonNodes) {
                long time = toUnixTimestamp(jsonNodes, timestampField);
                return time;
            }
        });
    }

    /**
     * field by处理,现只支持一个key
     *
     * @param dataStream
     * @param keyByFields
     * @return
     */
    private DataStream<ObjectNode> processKeyByField(DataStream<ObjectNode> dataStream, List<String> keyByFields) {
        final String key = keyByFields.get(0);
        DataStream<ObjectNode> keyedStream = dataStream.keyBy(new KeySelector<ObjectNode, String>() {
            @Override
            public String getKey(ObjectNode jsonNodes) throws Exception {
                return jsonNodes.get(key).asText();
            }
        });
        return keyedStream;
    }

    private static long toUnixTimestamp(ObjectNode jsonNodes, String timestampField) {
        String timestr = jsonNodes.get(timestampField).asText();
        long time = 0;
        try {
            time = DateUtil.unixTimestamp(timestr);
        } catch (Exception e) {
            logger.error("metric=eagle-assignWatermarker||timesrc={}||error=", timestr, e);
        }
        //logger.info("metric=eagle-assignWatermarker||timestr={}||timeunix={}", timestr, time);
        return time;
    }
}
