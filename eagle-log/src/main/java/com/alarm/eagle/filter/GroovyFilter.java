package com.alarm.eagle.filter;

import com.fasterxml.jackson.databind.node.ObjectNode;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

/**
 * Created by luxiaoxun on 2020/01/28.
 */
public class GroovyFilter implements Filter {
    private static String template =  "" +
            "package com.alarm.eagle.filter;" +
            "import com.fasterxml.jackson.databind.node.ObjectNode;" +
            "def match(ObjectNode o){[exp]}";

    private static String method = "match";

    private String filterExp;

    private transient GroovyObject filterObj;

    public GroovyFilter(String filterExp) throws Exception {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        GroovyClassLoader classLoader = new GroovyClassLoader(parent);
        Class clazz = classLoader.parseClass(template.replace("[exp]", filterExp));
        filterObj = (GroovyObject)clazz.newInstance();
    }

    public boolean filter(ObjectNode objectNode) {
        return (boolean)filterObj.invokeMethod(method, objectNode);
    }
}
