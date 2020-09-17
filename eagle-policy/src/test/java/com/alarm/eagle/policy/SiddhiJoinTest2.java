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

import java.util.Date;

public class SiddhiJoinTest2 {

    private static final Logger logger = LoggerFactory.getLogger(SiddhiJoinTest2.class);

    @Test
    public void testSiddhi() throws InterruptedException {
        // Creating Siddhi Manager
        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@App:name('Test') " +
                "define stream cseEventStream (symbol string, price float, volume long); " +
                "@info(name = 'query1') " +
                "from cseEventStream[symbol=='IBM']#window.time(1 min) as a join cseEventStream#window.time(1 min) as b " + "\n" +
                "on a.symbol==b.symbol and a.volume==100 and b.volume==400 " +
                "select a.symbol, a.price, b.price as price_b " +
                "insert into outputStream ;";

        System.out.println(siddhiApp);

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                System.out.print("query1");
                EventPrinter.print(timestamp, inEvents, removeEvents);
            }
        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("cseEventStream");

        siddhiAppRuntime.start();

        System.out.println(new Date());
        inputHandler.send(new Object[]{"IBM", 76.6f, 400L});
        Thread.sleep(1 * 1000);
        System.out.println(new Date());
        inputHandler.send(new Object[]{"IBM", 700f, 100L});
        siddhiAppRuntime.shutdown();

        siddhiManager.shutdown();

    }

}
