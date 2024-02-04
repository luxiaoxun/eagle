package com.alarm.eagle.sink.es;

import com.alarm.eagle.log.LogEntry;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.elasticsearch.action.index.IndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by luxiaoxun on 2020/01/28.
 */
public class EsSinkFunction implements ElasticsearchSinkFunction<LogEntry> {
    private static final Logger logger = LoggerFactory.getLogger(EsSinkFunction.class);
    private String indexPostfix = "";

    public EsSinkFunction(String indexPostfix) {
        this.indexPostfix = indexPostfix;
    }

    @Override
    public void process(LogEntry log, RuntimeContext runtimeContext, RequestIndexer requestIndexer) {
        log.setEsIndexPrefix(indexPostfix);
        // Use log id as ES doc id
        requestIndexer.add(new IndexRequest(log.generateIndexName())
                .id(log.getId())
                .source(log.toJSON().toString()));
    }
}
