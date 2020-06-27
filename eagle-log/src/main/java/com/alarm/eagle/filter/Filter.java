package com.alarm.eagle.filter;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by luxiaoxun on 2020/01/28.
 */
public interface Filter {
    public boolean filter(ObjectNode objectNode);
}
