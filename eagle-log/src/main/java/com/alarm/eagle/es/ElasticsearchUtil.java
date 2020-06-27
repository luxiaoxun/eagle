package com.alarm.eagle.es;

import org.apache.http.HttpHost;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luxiaoxun on 2020/01/28.
 */
public class ElasticsearchUtil {
    public static List<HttpHost> getEsAddresses(String hosts) {
        String[] hostList = hosts.split(",");
        List<HttpHost> addresses = new ArrayList<>();
        for (String host : hostList) {
            String[] parts = host.split(":", 2);
            addresses.add(new HttpHost(parts[0], parts.length >= 2 ? Integer.parseInt(parts[1]) : 9200));
        }
        return addresses;
    }
}
