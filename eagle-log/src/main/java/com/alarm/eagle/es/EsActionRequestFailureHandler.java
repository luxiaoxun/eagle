package com.alarm.eagle.es;

import org.apache.flink.streaming.connectors.elasticsearch.ActionRequestFailureHandler;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.util.ExceptionUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * Created by luxiaoxun on 2020/01/28.
 */
public class EsActionRequestFailureHandler implements ActionRequestFailureHandler {
    private static final Logger logger = LoggerFactory.getLogger(EsActionRequestFailureHandler.class);

    @Override
    public void onFailure(ActionRequest actionRequest, Throwable throwable, int i, RequestIndexer requestIndexer) throws Throwable {
        if (ExceptionUtils.findThrowable(throwable, ElasticsearchException.class).isPresent()) {
            IndexRequest indexRequest = (IndexRequest) actionRequest;
            logger.error("Error log:" + indexRequest.index() + " source: " + indexRequest.sourceAsMap() + " exception: " + throwable.toString());
        } else if (ExceptionUtils.findThrowable(throwable, ElasticsearchParseException.class).isPresent()) {
            IndexRequest indexRequest = (IndexRequest) actionRequest;
            logger.error("Error log:" + indexRequest.index() + " source: " + indexRequest.sourceAsMap() + " exception: " + throwable.toString());
        } else if (ExceptionUtils.findThrowable(throwable, IOException.class).isPresent()) {
            Optional<IOException> exp = ExceptionUtils.findThrowable(throwable, IOException.class);
            IOException ioExp = exp.get();
            if (ioExp != null && ioExp.getMessage() != null && ioExp.getMessage().contains("max retry timeout")) {
                logger.error(ioExp.getMessage());
            }
        } else {
            IndexRequest indexRequest = (IndexRequest) actionRequest;
            logger.warn("Error log:" + indexRequest.index() + " source: " + indexRequest.sourceAsMap() + " exception: " + throwable.toString());
            requestIndexer.add(indexRequest);
        }
    }
}
