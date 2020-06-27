package com.alarm.eagle.redis;

import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import java.util.Map;

/**
 * Created by luxiaoxun on 2020/06/15.
 */
public class LogStatWindowFunction extends ProcessWindowFunction<LogStatAccumulator, LogStatWindowResult, String, TimeWindow> {
    @Override
    public void process(String str, Context context, Iterable<LogStatAccumulator> iterable, Collector<LogStatWindowResult> collector) throws Exception {
        LogStatAccumulator acc = iterable.iterator().next();
        String key = acc.getKey();
        long count = acc.getCount();
        long startTime = context.window().getStart();
        long endTime = context.window().getEnd();
        Map<String, Long> ipMap = acc.getIpMap();
        collector.collect(new LogStatWindowResult(key, count, startTime, endTime, ipMap));
    }
}
