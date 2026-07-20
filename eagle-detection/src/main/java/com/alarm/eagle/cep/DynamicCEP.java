package com.alarm.eagle.cep;

import com.alarm.eagle.cep.operator.DynamicCepOperatorFactory;
import com.alarm.eagle.cep.processor.PatternProcessorDiscovererFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.functions.NullByteKeySelector;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;

/**
 * Dynamic CEP API entry point (counterpart to the official {@code CEP.java}).
 *
 * <p>Usage:
 * <pre>{@code
 * SingleOutputStreamOperator<String> output = DynamicCEP.dynamicPatterns(
 *     keyedStream,
 *     new JDBCPeriodicPatternProcessorDiscovererFactory<>(
 *         jdbcUrl, jdbcDriver, tableName, null, intervalMs),
 *     false,  // isProcessingTime
 *     TypeInformation.of(new TypeHint<String>() {}));
 * }</pre>
 */
public class DynamicCEP {

    /**
     * ★ Create a dynamic CEP stream.
     *
     * @param input             input event stream (must be KeyedStream, otherwise auto keyBy)
     * @param discovererFactory rule discoverer factory
     * @param isProcessingTime  true=ProcessingTime, false=EventTime
     * @param outTypeInfo       output type information
     * @param <T>  event type
     * @param <R>  output type
     * @return match result stream
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T, R> SingleOutputStreamOperator<R> dynamicPatterns(
            DataStream<T> input,
            PatternProcessorDiscovererFactory<T> discovererFactory,
            boolean isProcessingTime,
            TypeInformation<R> outTypeInfo) {

        DynamicCepOperatorFactory<T, R> factory =
                new DynamicCepOperatorFactory<>(
                        discovererFactory,
                        input.getType().createSerializer(
                                input.getExecutionConfig()),
                        isProcessingTime);

        // Generic type inference may fail across interface hierarchies; use raw type to bypass
        DataStream rawStream = input;
        if (rawStream instanceof KeyedStream) {
            return ((KeyedStream) rawStream)
                    .transform("DynamicCepOperator", outTypeInfo,
                            (org.apache.flink.streaming.api.operators
                                    .OneInputStreamOperatorFactory) factory);
        }
        return rawStream.keyBy(new NullByteKeySelector())
                .transform("DynamicCepOperator", outTypeInfo,
                        (org.apache.flink.streaming.api.operators
                                .OneInputStreamOperatorFactory) factory)
                .forceNonParallel();
    }
}
