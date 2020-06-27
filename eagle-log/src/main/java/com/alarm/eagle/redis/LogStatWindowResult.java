package com.alarm.eagle.redis;

import org.apache.flink.api.java.tuple.Tuple5;
import java.util.Map;

/**
 * Created by luxiaoxun on 2020/06/15.
 */
public class LogStatWindowResult extends Tuple5<String, Long, Long, Long, Map<String, Long>> {
    private static final long serialVersionUID = 1L;

    public LogStatWindowResult(String key, long count, long startTime, long endTime, Map<String, Long> ipMap) {
        super(key, count, startTime, endTime, ipMap);
    }

    public String getKey() {
        return this.f0;
    }

    public long getCount() {
        return this.f1;
    }

    public long getStartTime() {
        return this.f2;
    }

    public long getEndTime() {
        return this.f3;
    }

    public Map<String, Long> getIpMap() {
        return this.f4;
    }
}
