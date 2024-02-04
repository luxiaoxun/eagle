package com.alarm.eagle.sink.redis;

import org.apache.flink.api.java.tuple.Tuple3;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by luxiaoxun on 2020/06/15.
 */
public class LogStatAccumulator extends Tuple3<String, Long, Map<String, Long>> {
    private static final long serialVersionUID = 1L;

    public LogStatAccumulator() {
        super("", 0L, new HashMap<>(2048));
    }

    public LogStatAccumulator(String key, long count, Map<String, Long> ipMap) {
        super(key, count, ipMap);
    }

    public String getKey() {
        return this.f0;
    }

    public void setKey(String key) {
        this.f0 = key;
    }

    public long getCount() {
        return this.f1;
    }

    public void addCount(long count) {
        this.f1 += count;
    }

    public Map<String, Long> getIpMap() {
        return this.f2;
    }

    public void addIp(String ip) {
        if (this.f2.containsKey(ip)) {
            this.f2.put(ip, this.f2.get(ip) + 1);
        } else {
            this.f2.put(ip, 1L);
        }
    }

    public void addIpMap(Map<String, Long> ipMap) {
        this.f2.forEach((key, value) -> ipMap.merge(key, value, (v1, v2) -> v1 + v2));
    }
}
