package com.alarm.eagle.alert.filter;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by skycrab on 18/2/5.
 */
public interface Filter {
    public boolean filter(ObjectNode objectNode);
}
