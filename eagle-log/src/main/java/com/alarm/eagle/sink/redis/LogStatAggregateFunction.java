package com.alarm.eagle.sink.redis;

import com.alarm.eagle.log.LogEntry;
import org.apache.flink.api.common.functions.AggregateFunction;

/**
 * Created by luxiaoxun on 2020/06/15.
 */
public class LogStatAggregateFunction implements AggregateFunction<LogEntry, LogStatAccumulator, LogStatAccumulator> {
    private static final long serialVersionUID = 1L;

    @Override
    public LogStatAccumulator createAccumulator() {
        return new LogStatAccumulator();
    }

    @Override
    public LogStatAccumulator add(LogEntry logEntry, LogStatAccumulator logStatAccumulator) {
        if (logStatAccumulator.getKey().isEmpty()) {
            logStatAccumulator.setKey(logEntry.getIndex());
        }
        logStatAccumulator.addCount(1);
        String ip = logEntry.getIp();
        if (ip == null) {
            ip = "127.0.0.1";
        }
        logStatAccumulator.addIp(ip);
        return logStatAccumulator;
    }

    @Override
    public LogStatAccumulator getResult(LogStatAccumulator logStatAccumulator) {
        return logStatAccumulator;
    }

    @Override
    public LogStatAccumulator merge(LogStatAccumulator acc1, LogStatAccumulator acc2) {
        if (acc1.getKey().isEmpty()) {
            acc1.setKey(acc2.getKey());
        }
        acc1.addCount(acc2.getCount());
        acc1.addIpMap(acc2.getIpMap());
        return acc1;
    }
}
