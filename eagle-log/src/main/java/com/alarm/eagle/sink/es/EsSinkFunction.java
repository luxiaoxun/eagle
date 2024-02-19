package com.alarm.eagle.sink.es;

import com.alarm.eagle.log.LogEvent;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.elasticsearch.action.index.IndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by luxiaoxun on 2020/01/28.
 */
public class EsSinkFunction implements ElasticsearchSinkFunction<LogEvent> {
    private static final Logger logger = LoggerFactory.getLogger(EsSinkFunction.class);
    private String indexPostfix = "";

    public EsSinkFunction(String indexPostfix) {
        this.indexPostfix = indexPostfix;
    }

    @Override
    public void process(LogEvent log, RuntimeContext runtimeContext, RequestIndexer requestIndexer) {
        log.setIndexPostfix(indexPostfix);
        // Use log id as ES doc id
        String indexName = log.generateIndexName();
        logger.info("Index name: {}", indexName);
        requestIndexer.add(new IndexRequest(indexName)
                .id(log.getId())
                .source(log));
    }
}
