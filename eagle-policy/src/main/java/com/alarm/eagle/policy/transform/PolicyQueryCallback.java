package com.alarm.eagle.policy.transform;

import com.alarm.eagle.model.DataSink;
import com.alarm.eagle.util.DateUtil;
import com.alarm.eagle.util.JsonUtil;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.query.api.definition.Attribute;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by skycrab on 18/1/8.
 */
public class PolicyQueryCallback extends QueryCallback {
    private static final Logger logger = LoggerFactory.getLogger(PolicyQueryCallback.class);

    private CallbackCtx ctx;

    public PolicyQueryCallback(CallbackCtx ctx) {
        this.ctx = ctx;
    }

    @Override
    public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
        StreamRecord<DataSink> reusableRecord = new StreamRecord<>(null, 0L);
        for (Event event : inEvents) {
            int i = 0;
            Map<String, Object> result = new HashMap<>(ctx.getAttributeList().size());
            for (Attribute attribute : ctx.getAttributeList()) {
                result.put(attribute.getName(), event.getData(i));
                i++;
            }
            String siddhiTime = DateUtil.fromUnixtime(timestamp);
            result.put("siddhiReceiveTime", siddhiTime);
            ctx.getDataSink().setData(JsonUtil.encode(result));

            logger.info("metric=eagle-receive||timestamp={}||dataSink={}", siddhiTime, ctx.getDataSink());
            reusableRecord.replace(ctx.getDataSink(), event.getTimestamp());
            ctx.getOutput().collect(reusableRecord);
        }
    }
}
