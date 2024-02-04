package com.alarm.eagle.alert.filter;

import com.fasterxml.jackson.databind.node.ObjectNode;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

/**
 * Created by skycrab on 18/2/5.
 */
public class GroovyFilter implements Filter {
    private static String template = "" +
            "package com.alarm.eagle.policy.filter;" +
            "import com.fasterxml.jackson.databind.node.ObjectNode;" +
            "def match(ObjectNode o){[exp]}";

    private static String method = "match";

    private String filterExp;

    private transient GroovyObject filterObj;

    public GroovyFilter(String filterExp) throws Exception {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        GroovyClassLoader classLoader = new GroovyClassLoader(parent);
        Class clazz = classLoader.parseClass(template.replace("[exp]", filterExp));
        filterObj = (GroovyObject) clazz.newInstance();
    }

    public boolean filter(ObjectNode objectNode) {
        return (boolean) filterObj.invokeMethod(method, objectNode);
    }
}
