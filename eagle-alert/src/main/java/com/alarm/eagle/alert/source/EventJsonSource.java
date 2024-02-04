package com.alarm.eagle.alert.source;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.alarm.eagle.util.JsonUtil;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by skycrab on 17/12/22.
 */
public class EventJsonSource implements SourceFunction<ObjectNode> {
    private volatile int i = 0;
    private String streamId;
    private volatile boolean isRunning = true;

    public EventJsonSource(String streamId) {
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
    public void run(SourceContext<ObjectNode> sourceContext) throws Exception {
        Event[] events = streams.get(streamId);
        while (isRunning) {
            sourceContext.collect(JsonUtil.convertValue(events[i % events.length], ObjectNode.class));
            TimeUnit.SECONDS.sleep(10);
            i++;
        }
    }

    @Override
    public void cancel() {
        isRunning = false;
    }
}
