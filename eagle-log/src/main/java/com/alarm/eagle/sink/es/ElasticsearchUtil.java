package com.alarm.eagle.sink.es;

import com.alarm.eagle.log.LogEvent;
import com.alarm.eagle.util.StringUtil;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luxiaoxun on 2020/01/28.
 */
public class ElasticsearchUtil {
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchUtil.class);

    public static List<HttpHost> getEsAddresses(String hosts) {
        List<HttpHost> addresses = new ArrayList<>();
        String[] hostList = hosts.split(",");
        for (String host : hostList) {
            String[] parts = host.split(":", 2);
            addresses.add(new HttpHost(parts[0], parts.length >= 2 ? Integer.parseInt(parts[1]) : 9200));
        }
        return addresses;
    }

    public static IndexRequest createIndexRequest(LogEvent logEvent, String indexPostfix) {
        if (!StringUtil.isEmpty(indexPostfix)) {
            logEvent.setIndexPostfix(indexPostfix);
        }
        // Use log id as ES doc id
        String indexName = logEvent.generateIndexName();
        logger.debug("Index name: {}", indexName);
        return Requests.indexRequest()
                .index(indexName)
                .id(logEvent.getId())
                .source(logEvent);
    }

}
