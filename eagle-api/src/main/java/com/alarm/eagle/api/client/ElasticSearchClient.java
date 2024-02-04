package com.alarm.eagle.api.client;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Duration;
@Component
@Lazy
public class ElasticSearchClient {
    private static Logger logger = LogManager.getLogger(ElasticSearchClient.class);

    @Value("${es.address}")
    private String esAddress;

    @Value("${es.auth.enabled:false}")
    private boolean authEnabled;

    @Value("${es.auth.username:}")
    private String username;

    @Value("${es.auth.password:}")
    private String password;

    // ES Client
    private RestHighLevelClient client;

    public RestHighLevelClient getClient() {
        return client;
    }

    @PostConstruct
    public void init() throws UnknownHostException {
        logger.info("es.address: " + esAddress);
        String[] hostPort = esAddress.split(":");
        RestClientBuilder builder = RestClient.builder(new HttpHost(hostPort[0], Integer.parseInt(hostPort[1])));
        builder.setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder.setConnectTimeout(5000).setSocketTimeout(60000));
        builder.setHttpClientConfigCallback(httpClientConfig ->
                httpClientConfig.setKeepAliveStrategy((response, context) -> Duration.ofMinutes(5).toMillis()));

        if (authEnabled && StringUtils.hasLength(username) && StringUtils.hasLength(password)) {
            logger.info("es auth enabled");
            builder.setHttpClientConfigCallback(httpClientConfig -> {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
                return httpClientConfig.setDefaultCredentialsProvider(credentialsProvider);
            });
        }

        client = new RestHighLevelClient(builder);
    }

    @PreDestroy
    public void close() {
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                logger.error(e.toString());
            }
        }
    }
}
