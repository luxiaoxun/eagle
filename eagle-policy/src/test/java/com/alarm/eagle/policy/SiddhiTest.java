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

public class SiddhiTest {

    private static final Logger logger = LoggerFactory.getLogger(SiddhiTest.class);

    @Test
    public void testSiddhi() throws InterruptedException {
        // Creating Siddhi Manager
        SiddhiManager siddhiManager = new SiddhiManager();

        String siddhiApp = "" +
                "@App:name('Test')" +
                "define stream cseEventStream (symbol_symbol string, price float, volume long); " +
                "" +
                "@info(name = 'query1') " +
                "from cseEventStream[price<51 and price/volume>1] " +
                "select symbol_symbol,price,10 as c," +
                "math:power(2,3) as p," +
                "regex:matches('I.*', symbol_symbol) as aboutWSO2," +
                "time:extract('hour', '2014-3-11 02:23:44', 'yyyy-MM-dd hh:mm:ss') as hour," +
                "str:concat('a','b') as cc " +
                "insert into outputStream ;" +

                "@info(name = 'query2') " +
                "from cseEventStream[symbol_symbol == 'GOOG'] " +
                "select symbol_symbol,price " +
                "insert into outputStream2 ;";


        //Generating runtime
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);

        //Adding callback to retrieve output events from query
        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                System.out.print("query1");
                EventPrinter.print(timestamp, inEvents, removeEvents);
            }
        });

        siddhiAppRuntime.addCallback("query2", new QueryCallback() {
            @Override
            public void receive(long timestamp, Event[] inEvents, Event[] removeEvents) {
                System.out.print("query2");
                EventPrinter.print(timestamp, inEvents, removeEvents);
            }
        });

        //Retrieving InputHandler to push events into Siddhi
        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("cseEventStream");

        //Starting event processing
        siddhiAppRuntime.start();

        //Sending events to Siddhi
        inputHandler.send(new Object[]{"IBM", 700f, 100L});
        inputHandler.send(new Object[]{"WSO2", 60.5f, 200L});
        inputHandler.send(new Object[]{"GOOG", 50f, 30L});
        inputHandler.send(new Object[]{"IBM", 76.6f, 400L});
        inputHandler.send(new Object[]{"WSO2", 45.6f, 50L});
        Thread.sleep(500);

        //Shutting down the runtime
        siddhiAppRuntime.shutdown();

        //Shutting down Siddhi
        siddhiManager.shutdown();

    }

}
