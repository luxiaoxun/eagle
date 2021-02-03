package com.alarm.eagle;

import com.alarm.eagle.config.AlarmConfigConstant;
import com.alarm.eagle.config.EagleAlarmProperties;
import com.alarm.eagle.functions.AverageAggregate;
import com.alarm.eagle.functions.DynamicAlertFunction;
import com.alarm.eagle.functions.DynamicKeyFunction;
import com.alarm.eagle.message.Alert;
import com.alarm.eagle.message.Descriptors;
import com.alarm.eagle.message.Transaction;
import com.alarm.eagle.rule.Rule;
import com.alarm.eagle.sink.AlertsSink;
import com.alarm.eagle.sink.CurrentRulesSink;
import com.alarm.eagle.sink.LatencySink;
import com.alarm.eagle.source.RulesSource;
import com.alarm.eagle.source.TransactionsSource;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FraudDetectionApp {
    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionApp.class);

    public static void main(String[] args) {
        try {
            ParameterTool params = ParameterTool.fromArgs(args);
            ParameterTool parameter = EagleAlarmProperties.getInstance(params).getParameter();
            showConf(parameter);

            // Build stream DAG
            StreamExecutionEnvironment env = getStreamExecutionEnvironment(parameter);
            DataStream<Rule> rulesUpdateStream = getRulesUpdateStream(env, parameter);
            DataStream<Transaction> transactions = getTransactionsStream(env, parameter);
            BroadcastStream<Rule> rulesStream = rulesUpdateStream.broadcast(Descriptors.rulesDescriptor);

            // Processing pipeline setup
            DataStream<Alert> alerts = transactions
                    .connect(rulesStream)
                    .process(new DynamicKeyFunction())
                    .uid("DynamicKeyFunction")
                    .name("Dynamic Partitioning Function")
                    .keyBy((keyed) -> keyed.getKey())
                    .connect(rulesStream)
                    .process(new DynamicAlertFunction())
                    .uid("DynamicAlertFunction")
                    .name("Dynamic Rule Evaluation Function");

            SingleOutputStreamOperator<Alert> alertStream = (SingleOutputStreamOperator<Alert>) alerts;
            DataStream<String> allRuleEvaluations = alertStream.getSideOutput(Descriptors.demoSinkTag);
            DataStream<Long> latency = alertStream.getSideOutput(Descriptors.latencySinkTag);
            DataStream<Rule> currentRules = alertStream.getSideOutput(Descriptors.currentRulesSinkTag);

            alerts.print().name("Alert STDOUT Sink");
            allRuleEvaluations.print().setParallelism(1).name("Rule Evaluation Sink");

            DataStream<String> alertsJson = AlertsSink.alertsStreamToJson(alerts);
            DataStream<String> currentRulesJson = CurrentRulesSink.rulesStreamToJson(currentRules);
            currentRulesJson.print();

            alertsJson.addSink(AlertsSink.createAlertsSink(parameter)).setParallelism(1).name("Alerts JSON Sink");
            currentRulesJson.addSink(CurrentRulesSink.createRulesSink(parameter)).setParallelism(1);

            DataStream<String> latencies = latency.timeWindowAll(Time.seconds(10))
                    .aggregate(new AverageAggregate()).map(String::valueOf);
            latencies.addSink(LatencySink.createLatencySink(parameter));

            env.getConfig().setGlobalJobParameters(parameter);
            env.execute("eagle-alarm");

        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    private static DataStream<Transaction> getTransactionsStream(StreamExecutionEnvironment env, ParameterTool parameter) {
        // Data stream setup
        SourceFunction<String> transactionSource = TransactionsSource.createTransactionsSource(parameter);
        int sourceParallelism = parameter.getInt(AlarmConfigConstant.KAFKA_TOPIC_PARALLELISM);
        int orderness = parameter.getInt(AlarmConfigConstant.TRANSACTIONS_OUT_OF_ORDERNESS);
        DataStream<String> transactionsStringsStream = env.addSource(transactionSource).name("Transactions Source")
                .setParallelism(sourceParallelism);
        DataStream<Transaction> transactionsStream =
                TransactionsSource.stringsStreamToTransactions(transactionsStringsStream);
        return transactionsStream.assignTimestampsAndWatermarks(
                new SimpleBoundedOutOfOrdernessTimestampExtractor<>(orderness));
    }

    private static class SimpleBoundedOutOfOrdernessTimestampExtractor<T extends Transaction>
            extends BoundedOutOfOrdernessTimestampExtractor<T> {

        public SimpleBoundedOutOfOrdernessTimestampExtractor(int maxOutOfOrderness) {
            super(Time.of(maxOutOfOrderness, TimeUnit.MILLISECONDS));
        }

        @Override
        public long extractTimestamp(T element) {
            return element.getEventTime();
        }
    }

    private static DataStream<Rule> getRulesUpdateStream(StreamExecutionEnvironment env, ParameterTool parameter) {
        String name = "Rule Source";
        SourceFunction<String> rulesSource = RulesSource.createRulesSource(parameter);
        DataStream<String> rulesStrings =
                env.addSource(rulesSource).name(name).setParallelism(1);
        return RulesSource.stringsStreamToRules(rulesStrings);
    }

    private static StreamExecutionEnvironment getStreamExecutionEnvironment(ParameterTool parameter) {
        StreamExecutionEnvironment env = null;
        int globalParallelism = parameter.getInt(AlarmConfigConstant.FLINK_PARALLELISM);
        if (parameter.get(AlarmConfigConstant.FLINK_MODE).equals(AlarmConfigConstant.MODE_DEV)) {
            env = StreamExecutionEnvironment.createLocalEnvironment();
            globalParallelism = 1;
        } else {
            env = StreamExecutionEnvironment.getExecutionEnvironment();
        }
        env.setParallelism(globalParallelism);
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        //checkpoint
        boolean enableCheckpoint = parameter.getBoolean(AlarmConfigConstant.FLINK_ENABLE_CHECKPOINT, false);
        if (enableCheckpoint) {
            env.enableCheckpointing(60000L);
            CheckpointConfig config = env.getCheckpointConfig();
            config.setMinPauseBetweenCheckpoints(30000L);
            config.setCheckpointTimeout(10000L);
            //RETAIN_ON_CANCELLATION则在job cancel的时候会保留externalized checkpoint state
            config.enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.DELETE_ON_CANCELLATION);
        }

        return env;
    }

    private static void showConf(ParameterTool parameter) {
        logger.info("Show " + parameter.getNumberOfParameters() + " config parameters");
        Configuration configuration = parameter.getConfiguration();
        Map<String, String> map = configuration.toMap();
        for (String key : map.keySet()) {
            logger.info(key + ":" + map.get(key));
        }
    }
}
