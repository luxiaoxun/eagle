package com.alarm.eagle.policy.source;

import com.alarm.eagle.util.JsonUtil;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by skycrab on 17/12/22.
 */
public class JsonStringSource implements SourceFunction<String> {
    private volatile int i = 0;
    private String streamId;
    private volatile boolean isRunning = true;

    public JsonStringSource(String streamId) {
        this.streamId = streamId;
    }

    public static Event[] events1 = {
            new Event("2017-01-01 00:00:00", 30.5, 1),
            new Event("2017-01-01 00:00:10", 30.8, 1),
            new Event("2017-01-01 00:01:40", 40.8, 1),
            new Event("2017-01-01 00:02:20", 20.8, 1),
            new Event("2017-01-01 00:00:40", 60.8, 2),
    };

    public static Event[] events2 = {
            new Event("2017-01-01 00:00:00", 30.5, 2),
            new Event("2017-01-01 00:00:10", 30.8, 2),
            new Event("2017-01-01 00:00:20", 40.8, 2),
            new Event("2017-01-01 00:00:50", 20.8, 2)
    };

    public static Map<String, Event[]> streams = new HashMap<String, Event[]>();

    static {
        streams.put("stream1", events1);
        streams.put("stream2", events2);
    }

    @Override
    public void run(SourceContext<String> sourceContext) throws Exception {
        Event[] events = streams.get(streamId);
        while (isRunning) {
            sourceContext.collect(JsonUtil.encode(events[i % events.length]));
            TimeUnit.SECONDS.sleep(10);
            i++;
        }
    }

    @Override
    public void cancel() {
        isRunning = false;
    }
}
