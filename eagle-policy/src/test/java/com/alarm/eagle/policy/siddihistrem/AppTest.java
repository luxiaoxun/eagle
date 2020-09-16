package com.alarm.eagle.policy.siddihistrem;

import com.alarm.eagle.bean.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.alarm.eagle.policy.source.JsonSource;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * 启动程序
 */
public class AppTest {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();

        // enable event time processing
        environment.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        //task
        Task task = new Task();
        task.setTaskId(1);
        task.setTaskName("test");

        //datasource1
        Datasource datasource = new Datasource();
        datasource.setEventTimestampField("timestamp");
        datasource.setStreamId("stream1");

        //datasource2
        Datasource datasource2 = new Datasource();
        datasource2.setEventTimestampField("timestamp");
        datasource2.setStreamId("stream2");


        List<Datasource> datasourceList = new ArrayList<>();
        datasourceList.add(datasource);
        datasourceList.add(datasource2);

        task.setDatasourceList(datasourceList);

        //field
        List<Field> fieldList = new ArrayList<Field>() {{
            add(new Field("timestamp", "string"));
            add(new Field("temperature", "double"));
            add(new Field("rack", "int"));
        }};

        //stream define
        StreamDefine streamDefine = new StreamDefine();
        streamDefine.setStreamId("stream1");
        streamDefine.setFieldList(fieldList);

        StreamDefine streamDefine2 = new StreamDefine();
        streamDefine2.setStreamId("stream2");
        streamDefine2.setFieldList(fieldList);

        List<StreamDefine> streamDefineList = new ArrayList<>();
        streamDefineList.add(streamDefine);
        streamDefineList.add(streamDefine2);

        //app
        StreamApp streamApp = new StreamApp();
        streamApp.setStreamDefineList(streamDefineList);

        List<StreamApp> streamAppList = new ArrayList<>();
        streamAppList.add(streamApp);

        task.setStreamAppList(streamAppList);

        //构建带streamId的Datastream
        List<DataStream<Tuple2<String, ObjectNode>>> nameDatastreamList = new ArrayList<>();

        for (Datasource datasource1 : task.getDatasourceList()) {
            final String streamId = datasource1.getStreamId();
            DataStream<ObjectNode> dataStream = environment.addSource(new JsonSource(streamId)).assignTimestampsAndWatermarks(
                    new BoundedOutOfOrdernessTimestampExtractor<ObjectNode>(Time.minutes(1L)) {
                        @Override
                        public long extractTimestamp(ObjectNode jsonNodes) {
                            return jsonNodes.get(datasource1.getEventTimestampField()).asLong();
                        }
                    });
            DataStream<Tuple2<String, ObjectNode>> nameDatastream = dataStream.map(new MapFunction<ObjectNode, Tuple2<String, ObjectNode>>() {
                @Override
                public Tuple2<String, ObjectNode> map(ObjectNode row) throws Exception {
                    return Tuple2.of(streamId, row);
                }
            });
            nameDatastreamList.add(nameDatastream);
        }

        //将多个流合并
        DataStream<Tuple2<String, ObjectNode>> unionNameDatastrem = nameDatastreamList.get(0);
        for (int i = 1; i < nameDatastreamList.size(); i++) {
            unionNameDatastrem = unionNameDatastrem.union(nameDatastreamList.get(i));
        }

        TypeInformation<ObjectNode> typeInfo = TypeExtractor.createTypeInfo(ObjectNode.class);

        DataStream<ObjectNode> alertDatastream = unionNameDatastrem.transform("siddhi",
                typeInfo, new SiddhiObjectNodeTestOperator(task));
        alertDatastream.print();

        environment.execute();

        TimeUnit.SECONDS.sleep(5);

    }
}
