package com.alarm.eagle;

import com.alarm.eagle.util.DateUtil;
import org.junit.Assert;
import org.junit.Test;


/**
 * Created by luxiaoxun on 18/1/5.
 */
public class UtilTest {
    @Test
    public void testTime() {
        long t = DateUtil.unixTimestamp("2018-1-5 17:33:50");
        Assert.assertEquals(t, 1515144830000L);

        String d = DateUtil.fromUnixtime(1515144830000L);
        Assert.assertEquals(d, "2018-01-05 17:33:50");

        int h = DateUtil.hour("2018-03-08 00:00:02");
        int h2 = DateUtil.hour();
        System.out.println(h2);

    }

}
