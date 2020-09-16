package com.alarm.eagle.policy.filter;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by skycrab on 18/2/5.
 */
public interface Filter {
    public boolean filter(ObjectNode objectNode);
}
