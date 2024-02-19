package com.alarm.eagle.util;

import com.google.common.base.Charsets;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HttpUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000;
    private static final int DEFAULT_SO_TIMEOUT = 5000;
    private static final int DEFAULT_MAX_CONNECTION_PER_ROUTE = 32;
    private static final int DEFAULT_MAX_CONNECTIONS = 64;
    private static final int DEFAULT_RETRIES = 2;
    private static final int SUB_STRING_SIZE = 4096;
    private static HttpClient httpClient;
    private static final HttpConnectionManager httpConnectionManager = new MultiThreadedHttpConnectionManager();

    static {
        HttpConnectionManagerParams params = httpConnectionManager.getParams();
        params.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
        params.setSoTimeout(DEFAULT_SO_TIMEOUT);
        params.setDefaultMaxConnectionsPerHost(DEFAULT_MAX_CONNECTION_PER_ROUTE);
        params.setMaxTotalConnections(DEFAULT_MAX_CONNECTIONS);
        httpClient = new HttpClient(httpConnectionManager);
    }

    public HttpUtil() {
    }

    public static String doGet(String urlPath) {
        return doGet(urlPath, Charsets.UTF_8, (Map) null, (Map) null);
    }

    public static String doGet(String urlPath, Map<String, String> params) {
        return doGet(urlPath, Charsets.UTF_8, params, (Map) null);
    }

    public static String doGet(String urlPath, Map<String, String> params, Map<String, String> headers) {
        return doGet(urlPath, Charsets.UTF_8, params, headers);
    }

    public static String doGet(String urlPath, Charset charset, Map<String, String> params, Map<String, String> headers) {
        String response = null;
        try {
            GetMethod e = new GetMethod(urlPath);
            Map.Entry map;
            if (headers != null && !headers.isEmpty()) {
                Iterator stream = headers.entrySet().iterator();
                while (stream.hasNext()) {
                    map = (Map.Entry) stream.next();
                    e.addRequestHeader((String) map.getKey(), (String) map.getValue());
                }
            }

            if (params != null && !params.isEmpty()) {
                Iterator stream = params.entrySet().iterator();
                while (stream.hasNext()) {
                    map = (Map.Entry) stream.next();
                    e.getParams().setParameter((String) map.getKey(), map.getValue());
                }
            }

            e.getParams().setParameter("http.method.retry-handler", new DefaultHttpMethodRetryHandler());
            httpClient.executeMethod(e);
            if (e.getStatusCode() == 200) {
                InputStream stream1 = e.getResponseBodyAsStream();
                response = IOUtils.toString(stream1, charset);
            } else {
                logger.warn("http get status code error,status {}", e.getStatusCode());
            }
        } catch (Exception ex) {
            logger.error(ex.toString());
        }

        return response;
    }

    public static String doPost(String urlPath, Map<String, String> params) {
        return doPost(urlPath, (Charset) Charsets.UTF_8, (Map) params, (Map) null);
    }

    public static String doPost(String urlPath, Map<String, String> params, Map<String, String> headers) {
        return doPost(urlPath, Charsets.UTF_8, params, headers);
    }

    public static String doPost(String urlPath, Charset charset, Map<String, String> params, Map<String, String> headers) {
        String response = null;
        try {
            PostMethod e = new PostMethod(urlPath);
            Map.Entry m;
            if (params != null && !params.isEmpty()) {
                Iterator inputStream = params.entrySet().iterator();
                while (inputStream.hasNext()) {
                    m = (Map.Entry) inputStream.next();
                    e.addParameter((String) m.getKey(), (String) m.getValue());
                }
            }

            if (headers != null && !headers.isEmpty()) {
                Iterator inputStream = headers.entrySet().iterator();
                while (inputStream.hasNext()) {
                    m = (Map.Entry) inputStream.next();
                    e.addRequestHeader((String) m.getKey(), (String) m.getValue());
                }
            }

            httpClient.executeMethod(e);
            if (e.getStatusCode() == 200) {
                InputStream inputStream1 = e.getResponseBodyAsStream();
                response = IOUtils.toString(inputStream1, charset);
            } else {
                logger.warn("http post status code error,status {}", Integer.valueOf(e.getStatusCode()));
            }
        } catch (Exception ex) {
            logger.error(ex.toString());
        }

        return response;
    }

    public static String doPost(String urlPath, String content) {
        return doPost(urlPath, (String) content, (Charset) Charsets.UTF_8, (Map) null);
    }

    public static String doPost(String urlPath, String content, Map<String, String> headers) {
        return doPost(urlPath, content, Charsets.UTF_8, headers);
    }

    public static String doPostJson(String urlPath, String content) {
        HashMap headers = new HashMap();
        headers.put("Content-Type", "application/json");
        return doPost(urlPath, (String) content, (Charset) Charsets.UTF_8, headers);
    }

    public static String doPostJson(String urlPath, String content, Map<String, String> headers) {
        headers.put("Content-Type", "application/json");
        return doPost(urlPath, content, Charsets.UTF_8, headers);
    }

    public static String doPost(String urlPath, String content, Charset charset, Map<String, String> headers) {
        String response = null;
        try {
            PostMethod e = new PostMethod(urlPath);
            if (headers != null && !headers.isEmpty()) {
                Iterator requestEntity = headers.entrySet().iterator();
                while (requestEntity.hasNext()) {
                    Map.Entry inputStream = (Map.Entry) requestEntity.next();
                    e.addRequestHeader((String) inputStream.getKey(), (String) inputStream.getValue());
                }
            }

            StringRequestEntity requestEntity1 = new StringRequestEntity(content);
            e.setRequestEntity(requestEntity1);
            httpClient.executeMethod(e);
            if (e.getStatusCode() == 200) {
                InputStream inputStream1 = e.getResponseBodyAsStream();
                response = IOUtils.toString(inputStream1, charset);
            } else {
                logger.warn("http post status code error,status {}", e.getStatusCode());
            }
        } catch (Exception ex) {
            logger.error(ex.toString());
        }

        return response;
    }

}
