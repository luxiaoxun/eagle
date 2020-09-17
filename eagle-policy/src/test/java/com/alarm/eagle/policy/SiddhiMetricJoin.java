package com.alarm.eagle.policy;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.util.EventPrinter;

public class SiddhiMetricJoin {

    private static final Logger logger = LoggerFactory.getLogger(SiddhiMetricJoin.class);

    @Test
    public void testSiddhi() throws InterruptedException {
        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "@App:name('Test') " +
                "define stream stream1 (boardMetricId int, name string, current double, period string, tag string, tagName string); " +
                "from stream1#window.time(1 min) as a join stream1#window.time(1 min) as b " +
                "on a.tag==b.tag and a.boardMetricId==29 and a.period=='10' and b.boardMetricId==31 and b.period=='10' " +
                "select a.boardMetricId as boardMetricId_a, a.current as current_a, a.tagName as tagName_a, a.name as name_a, " +
                "b.boardMetricId as boardMetricId_b, b.current as current_b, b.tagName as tagName_b, b.name as name_b, " +
                "b.current/(b.current+a.current) as rate " +
                "insert into outputStream; " +
                "@info(name = 'query1') " +
                "from outputStream[rate>0.0458] " +
                "insert into outputStream2;";

        System.out.println(siddhiApp);

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timestamp, inEvents, removeEvents);
            }
        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("stream1");

        siddhiAppRuntime.start();

        inputHandler.send(new Object[]{29, "报价成功次数", 1751, "10", "all", "全部"});
        inputHandler.send(new Object[]{31, "报价失败次数", 84, "10", "all", "全部"});
        inputHandler.send(new Object[]{33, "失败率", 0.045776, "10", "all", "全部"});
        siddhiAppRuntime.shutdown();

        siddhiManager.shutdown();

    }

}
