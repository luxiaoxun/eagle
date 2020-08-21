package com.alarm.eagle.functions;

import com.alarm.eagle.message.TimestampAssignable;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.util.Collector;

public class TimeStamper<T extends TimestampAssignable<Long>> extends RichFlatMapFunction<T, T> {

    @Override
    public void flatMap(T value, Collector<T> out) throws Exception {
        value.assignIngestionTimestamp(System.currentTimeMillis());
        out.collect(value);
    }
}
