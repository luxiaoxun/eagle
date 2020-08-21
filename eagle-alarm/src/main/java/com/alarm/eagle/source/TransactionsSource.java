package com.alarm.eagle.source;

import com.alarm.eagle.config.AlarmConfigConstant;
import com.alarm.eagle.functions.JsonDeserializer;
import com.alarm.eagle.functions.JsonGeneratorWrapper;
import com.alarm.eagle.functions.TimeStamper;
import com.alarm.eagle.message.Transaction;

import java.util.Properties;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;

public class TransactionsSource {

    public static SourceFunction<String> createTransactionsSource(ParameterTool parameter) {
        String sourceType = parameter.get(AlarmConfigConstant.TRANSACTIONS_SOURCE_TYPE);
        Type transactionsSourceType = Type.valueOf(sourceType.toUpperCase());
        int transactionsPerSecond = parameter.getInt(AlarmConfigConstant.TRANSACTIONS_PER_SECOND);
        switch (transactionsSourceType) {
            case KAFKA:
                String kafkaBootstrapServers = parameter.get(AlarmConfigConstant.KAFKA_BOOTSTRAP_SERVERS);
                String kafkaGroupId = parameter.get(AlarmConfigConstant.KAFKA_GROUP_ID);
                String transactionsTopic = parameter.get(AlarmConfigConstant.KAFKA_TOPIC);

                Properties properties = new Properties();
                properties.setProperty("bootstrap.servers", kafkaBootstrapServers);
                properties.setProperty("group.id", kafkaGroupId);

                FlinkKafkaConsumer010<String> kafkaConsumer =
                        new FlinkKafkaConsumer010<>(transactionsTopic, new SimpleStringSchema(), properties);
                kafkaConsumer.setStartFromLatest();
                return kafkaConsumer;
            case GENERATOR:
                return new JsonGeneratorWrapper<>(new TransactionsGenerator(transactionsPerSecond));
            default:
                return new JsonGeneratorWrapper<>(new TransactionsGenerator(transactionsPerSecond));
        }
    }

    public static DataStream<Transaction> stringsStreamToTransactions(
            DataStream<String> transactionStrings) {
        return transactionStrings
                .flatMap(new JsonDeserializer<Transaction>(Transaction.class))
                .returns(Transaction.class)
                .flatMap(new TimeStamper<Transaction>())
                .returns(Transaction.class)
                .name("Transactions Deserialization");
    }

    public enum Type {
        GENERATOR("Transactions Source (generated locally)"),
        KAFKA("Transactions Source (Kafka)");

        private String name;

        Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
