package com.alarm.eagle.api.util;

import java.io.IOException;
import java.util.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by luxiaoxun on 18/1/17.
 */
public class HttpUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    private static final String CONTENT_TYPE = "application/json; charset=utf-8";

    public static String post(String url, String[] names, Object[] values) {
        String result = null;
        List<NameValuePair> formParams = new ArrayList<>();
        if (names != null) {
            if (values == null || names.length != values.length)
                throw new RuntimeException("参数和值的数组长度不相等");
            for (int i = 0; i < names.length; i++) {
                if (values[i] == null) {
                    values[i] = "";
                }
                NameValuePair param = new BasicNameValuePair(names[i], values[i].toString());
                formParams.add(param);
            }
        }

        //创建连接
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(url);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10000).setConnectionRequestTimeout(5000)
                .setSocketTimeout(10000).build();
        httppost.setConfig(requestConfig);
        UrlEncodedFormEntity uefEntity;
        try {
            uefEntity = new UrlEncodedFormEntity(formParams, "UTF-8");
            httppost.setEntity(uefEntity);
            CloseableHttpResponse response = httpClient.execute(httppost);
            try {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    result = EntityUtils.toString(entity, "UTF-8");
                }
            } finally {
                response.close();
            }
        } catch (Exception e) {
            logger.error("Http post error: " + e.toString());
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
            }
        }
        return result;
    }

    public static String postBody(String url, String body) {
        String result = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(url);
        try {
            httppost.setHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);
            httppost.setEntity(new StringEntity(body, "UTF-8"));
            CloseableHttpResponse response = httpClient.execute(httppost);
            try {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    result = EntityUtils.toString(entity, "UTF-8");
                }
            } finally {
                response.close();
            }
        } catch (Exception e) {
            logger.error("Http post body error: " + e.toString());
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
            }
        }
        return result;
    }

    public static String putBody(String url, String body, String username, String password) {
        String result = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPut httppost = new HttpPut(url);
        try {
            String userMsg = username + ":" + password;
            String bas64UserMsg = Base64.getEncoder().encodeToString(userMsg.getBytes());
            httppost.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + bas64UserMsg);
            httppost.setHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);
            httppost.setEntity(new StringEntity(body, "UTF-8"));
            CloseableHttpResponse response = httpClient.execute(httppost);
            try {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    result = EntityUtils.toString(entity, "UTF-8");
                }
            } finally {
                response.close();
            }
        } catch (Exception e) {
            logger.error("Http put body error: " + e.toString());
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
            }
        }
        return result;
    }

    public static String get(String url) {
        String result = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(url);
        try {
            CloseableHttpResponse response = httpClient.execute(httpget);
            try {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    result = EntityUtils.toString(entity, "UTF-8");
                }
            } finally {
                response.close();
            }
        } catch (Exception e) {
            logger.error("Http get error: " + e.toString());
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
            }
        }
        return result;
    }
}