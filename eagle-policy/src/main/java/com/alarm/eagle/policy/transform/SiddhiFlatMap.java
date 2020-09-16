package com.alarm.eagle.policy.transform;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.alarm.eagle.policy.filter.Filter;
import com.alarm.eagle.policy.filter.GroovyFilter;
import com.alarm.eagle.util.DateUtil;
import com.alarm.eagle.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

/**
 * 1.格式转换为json
 * 2.表达式过滤
 * <p>
 * Created by skycrab on 18/2/5.
 */
public class SiddhiFlatMap extends RichFlatMapFunction<String, ObjectNode> {
    private transient Filter filter;
    private String filterExp;

    public SiddhiFlatMap(String filterExp) {
        this.filterExp = filterExp;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        if (StringUtils.isNotBlank(filterExp)) {
            filter = new GroovyFilter(filterExp);
        }
    }

    private boolean match(ObjectNode objectNode) {
        if (filter != null) {
            return filter.filter(objectNode);
        }
        return true;
    }

    @Override
    public void flatMap(String s, Collector<ObjectNode> collector) throws Exception {
        ObjectNode objectNode = JsonUtil.decode(s, ObjectNode.class);
        if (objectNode != null && match(objectNode)) {
            objectNode.put("siddhiHour", DateUtil.hour());
            collector.collect(objectNode);
        }
    }
}
