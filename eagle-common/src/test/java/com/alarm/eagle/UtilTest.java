package com.alarm.eagle;

import com.alarm.eagle.util.DateUtil;
import com.alarm.eagle.util.Md5Util;
import com.alarm.eagle.util.RegexUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;


/**
 * Created by luxiaoxun on 18/1/5.
 */
public class UtilTest {
    @Test
    public void testTime() {
        long t = DateUtil.unixTimestamp("2018-1-5 17:33:50");
        Assert.assertEquals(t, 1515144830000L);

        String d = DateUtil.toUnixTimeString(1515144830000L);
        Assert.assertEquals(d, "2018-01-05 17:33:50");

        int h = DateUtil.hour("2018-03-08 00:00:02");
        Assert.assertEquals(h, 0);
    }

    @Test
    public void testRegex() {
        String msg = "<190>Mar 26 14:41:42 2020 FW %%10FILTER/6/FILTER_ZONE_IPV4_EXECUTION: SrcZoneName(1025)=Trust;DstZoneName(1035)=Untrust;Type(1067)=ACL;SecurityPolicy(1072)=4;RuleID(1078)=4;Protocol(1001)=UDP;Application(1002)=general_udp;SrcIPAddr(1003)=172.16.106.199;SrcPort(1004)=7980;DstIPAddr(1007)=119.85.167.166;DstPort(1008)=28060;MatchCount(1069)=5;Event(1048)=Permit;";
        System.out.println(RegexUtil.extractString("<(\\d+)>", msg));
        System.out.println(RegexUtil.extractString("(\\w{3} \\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2})", msg));
        List<Pair<String, String>> pairs = RegexUtil.extractPairs("(\\w+\\(\\d+\\))=(\\w+);", msg);
        if (pairs != null && pairs.size() > 0) {
            pairs.stream().forEach(System.out::println);
        }
    }

    @Test
    public void testMd5() {
        System.out.println(Md5Util.MD5("aaaa"));
        System.out.println(Md5Util.MD5("1234"));
    }

}
