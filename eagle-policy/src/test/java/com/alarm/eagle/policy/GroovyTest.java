package com.alarm.eagle.policy;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.alarm.eagle.policy.filter.Filter;
import com.alarm.eagle.policy.filter.GroovyFilter;
import com.alarm.eagle.util.JsonUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by skycrab on 18/2/5.
 */
public class GroovyTest {
    @Test
    public void testFilter() throws Exception {
        ObjectNode objectNode = JsonUtil.createObjectNode();
        objectNode.put("period", 60);
        Filter filter = new GroovyFilter("o.hasNonNull(\"period\")");
        Assert.assertTrue(filter.filter(objectNode));

        Filter filter2 = new GroovyFilter("o.hasNonNull(\"period2\")");
        Assert.assertFalse(filter2.filter(objectNode));
    }
}
