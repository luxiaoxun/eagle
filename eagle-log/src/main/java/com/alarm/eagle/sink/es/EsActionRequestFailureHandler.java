package com.alarm.eagle.sink.es;

import org.apache.flink.streaming.connectors.elasticsearch.ActionRequestFailureHandler;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.util.ExceptionUtils;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.util.concurrent.EsRejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by luxiaoxun on 2020/01/28.
 */
public class EsActionRequestFailureHandler implements ActionRequestFailureHandler {
    private static final Logger logger = LoggerFactory.getLogger(EsActionRequestFailureHandler.class);

    @Override
    public void onFailure(ActionRequest actionRequest, Throwable throwable, int i, RequestIndexer requestIndexer) throws Throwable {
        if (ExceptionUtils.findThrowable(throwable, EsRejectedExecutionException.class).isPresent()) {
            // full queue; re-add document for indexing
            IndexRequest indexRequest = (IndexRequest) actionRequest;
            logger.warn("Re-add record into index: " + indexRequest.index() + " exception: " + throwable.toString());
            requestIndexer.add(indexRequest);
        } else if (ExceptionUtils.findThrowable(throwable, ElasticsearchParseException.class).isPresent()) {
            // malformed document; simply drop request without failing sink
            IndexRequest indexRequest = (IndexRequest) actionRequest;
            logger.error("Error log:" + indexRequest.index() + " source: " + indexRequest.sourceAsMap() + " exception: " + throwable.toString());
        } else {
            // for all other failures, fail the sink
            IndexRequest indexRequest = (IndexRequest) actionRequest;
            logger.error("Error log:" + indexRequest.index() + " source: " + indexRequest.sourceAsMap() + " exception: " + throwable.toString());
        }
    }
}
