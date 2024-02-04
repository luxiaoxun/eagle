package com.alarm.eagle.api.service;

import com.alarm.eagle.api.client.ElasticSearchClient;
import com.alarm.eagle.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.indices.*;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;

@Service
@Slf4j
public class EsIndexManager {
    @Autowired
    private ElasticSearchClient client;


    public void initIndexAndSetting() {
        //初始化索引模板
        createIndexTemplate();

    }

    public boolean createIndexTemplate() {
        log.info("Start to set index template for eagle log");
        boolean result = false;
        PutIndexTemplateRequest request = new PutIndexTemplateRequest("eagle-log-template");
        request.patterns(Arrays.asList("eagle_log-*"));
        request.settings(Settings.builder()
                        .put("index.refresh_interval", "1s")
                        .put("index.number_of_shards", 4)
                        .put("index.number_of_replicas", 0)
                        .put("index.analysis.analyzer.analyzer_keyword.filter", "lowercase")
                        .put("index.analysis.analyzer.analyzer_keyword.tokenizer", "keyword")
                ).mapping("{\n" +
                                "      \"dynamic_templates\" : [\n" +
                                "        {\n" +
                                "          \"strings_not_analyzed\" : {\n" +
                                "            \"mapping\" : {\n" +
                                "              \"type\" : \"keyword\"\n" +
                                "            },\n" +
                                "            \"match_mapping_type\" : \"string\"\n" +
                                "          }\n" +
                                "        }\n" +
                                "      ],\n" +
                                "      \"properties\" : {\n" +
                                "        \"message\" : {\n" +
                                "          \"type\" : \"text\",\n" +
                                "          \"fielddata\" : false,\n" +
                                "          \"analyzer\" : \"standard\"\n" +
                                "        },\n" +

                                "        \"device_ip\" : {\n" +
                                "          \"type\" : \"ip\"\n" +
                                "        },\n" +
                                "        \"src_ip\" : {\n" +
                                "          \"type\" : \"ip\"\n" +
                                "        },\n" +
                                "        \"dst_ip\" : {\n" +
                                "          \"type\" : \"ip\"\n" +
                                "        },\n" +
                                "        \"ip\" : {\n" +
                                "          \"type\" : \"ip\"\n" +
                                "        },\n" +
                                "        \"timestamp\" : {\n" +
                                "          \"format\" : \"yyyy-MM-dd HH:mm:ss.SSS\",\n" +
                                "          \"type\" : \"date\"\n" +
                                "        }\n" +
                                "      }\n" +
                                "    }",
                        XContentType.JSON)
                .order(0);
        try {
            AcknowledgedResponse response = client.getClient().indices().putTemplate(request, RequestOptions.DEFAULT);
            result = response.isAcknowledged();
        } catch (Exception ex) {
            log.error("Put index template error: " + ex.toString());
        }
        log.info("Set index template done");
        return result;
    }

    public boolean updateIndexSettings(String... indices) {
        log.info("Start to update index settings for {}", String.join(",", indices));
        boolean result = false;
        UpdateSettingsRequest request = new UpdateSettingsRequest(indices);
        request.settings(Settings.builder().put("index.refresh_interval", "1s"));
        try {
            AcknowledgedResponse response = client.getClient().indices().putSettings(request, RequestOptions.DEFAULT);
            result = response.isAcknowledged();
        } catch (Exception ex) {
            log.error("Update index settings error: " + ex.toString());
        }
        return result;
    }

    public List<String> getIndexNames(String indexName) {
        List<String> result = new ArrayList<>();
        if (ObjectUtils.isEmpty(indexName)) {
            return result;
        }
        try {
            GetIndexRequest request = new GetIndexRequest(indexName);
            GetIndexResponse getIndexResponse = client.getClient().indices().get(request, RequestOptions.DEFAULT);
            result = Arrays.asList(getIndexResponse.getIndices());
        } catch (Exception e) {
            log.error("Get index:{} error:{}", indexName, e);
        }
        return result;
    }

    public boolean checkIndexExist(String indexName) {
        if (ObjectUtils.isEmpty(indexName)) {
            return false;
        }
        try {
            GetIndexRequest request = new GetIndexRequest(indexName);
            return client.getClient().indices().exists(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            log.error("Check index exist error: " + e.toString());
            return false;
        }
    }

    public boolean createIndex(String indexName) {
        if (ObjectUtils.isEmpty(indexName)) {
            return false;
        }
        try {
            CreateIndexRequest request = new CreateIndexRequest(indexName);
            CreateIndexResponse createIndexResponse = client.getClient().indices().create(request, RequestOptions.DEFAULT);
            return createIndexResponse.isAcknowledged();
        } catch (Exception e) {
            log.error("Create index error: " + e.toString());
            return false;
        }
    }

    public boolean createIndexIfNotExist(String indexName) {
        if (ObjectUtils.isEmpty(indexName)) {
            return false;
        }
        try {
            if (!checkIndexExist(indexName)) {
                CreateIndexRequest request = new CreateIndexRequest(indexName);
                CreateIndexResponse createIndexResponse = client.getClient().indices().create(request, RequestOptions.DEFAULT);
                return createIndexResponse.isAcknowledged();
            } else {
                log.info("Index {} already exist", indexName);
                return true;
            }
        } catch (Exception e) {
            log.error("Create index error: " + e.toString());
            return false;
        }
    }

    public boolean createIndexAlias(String indexName, String indexAlias) {
        if (ObjectUtils.isEmpty(indexName) || ObjectUtils.isEmpty(indexAlias)) {
            return false;
        }
        IndicesAliasesRequest request = new IndicesAliasesRequest();
        IndicesAliasesRequest.AliasActions aliasAction =
                new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
                        .index(indexName)
                        .alias(indexAlias);
        request.addAliasAction(aliasAction);
        try {
            AcknowledgedResponse indicesAliasesResponse =
                    client.getClient().indices().updateAliases(request, RequestOptions.DEFAULT);
            return indicesAliasesResponse.isAcknowledged();
        } catch (Exception e) {
            log.error("Create index alias error: " + e.toString());
            return false;
        }
    }

    public Map<String, Set<AliasMetadata>> getIndexAlias(String indexAlias) {
        GetAliasesRequest request = null;
        if (ObjectUtils.isEmpty(indexAlias)) {
            request = new GetAliasesRequest();
        } else {
            request = new GetAliasesRequest(indexAlias);
        }
        try {
            GetAliasesResponse response = client.getClient().indices().getAlias(request, RequestOptions.DEFAULT);
            Map<String, Set<AliasMetadata>> aliases = response.getAliases();
            return aliases;
        } catch (Exception e) {
            log.error("Get index alias error: " + e.toString());
        }
        return Collections.emptyMap();
    }

    public JsonNode getIndexStats(String indexName) {
        JsonNode result = JsonUtil.createObjectNode();
        try {
            Request request = new Request("GET", "/" + indexName + "/_stats");
            Response response = client.getClient().getLowLevelClient().performRequest(request);
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);
            result = JsonUtil.decode(responseBody, JsonNode.class);
            return result;
        } catch (Exception e) {
            log.error("Get index stats error: " + e.toString());
        }
        return result;
    }

    public boolean deleteIndex(String indexName) {
        if (ObjectUtils.isEmpty(indexName)) {
            return false;
        }
        try {
            DeleteIndexRequest request = new DeleteIndexRequest(indexName);
            AcknowledgedResponse deleteIndexResponse = client.getClient().indices()
                    .delete(request, RequestOptions.DEFAULT);
            return deleteIndexResponse.isAcknowledged();
        } catch (Exception ex) {
            log.error("Delete index {} error: {}", indexName, ex);
            return false;
        }
    }
}
