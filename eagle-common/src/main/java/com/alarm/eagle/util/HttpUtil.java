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

    private static String mockRules = "[{\n" +
            "\t\"id\": 123,\n" +
            "\t\"appId\": \"059b0847-4fda-487a-ab85-7a5e625a8bd1\",\n" +
            "\t\"type\": \"logrules\",\n" +
            "\t\"script\": \"package logrules\\n\\nimport com.alarm.eagle.util.DateUtil;\\nimport com.alarm.eagle.log.LogEntry;\\nimport org.slf4j.Logger;\\nimport com.alarm.eagle.util.Md5Util;\\nimport com.alarm.eagle.util.RegexUtil\\nimport java.util.Date;\\n\\nglobal Logger LOG;\\n\\nrule \\\"eagle_log_1 log rules\\\"\\nno-loop true\\nsalience 100\\nwhen\\n    $log : LogEntry( index == \\\"eagle_log_1\\\", $msg : message)\\nthen\\n    LOG.debug(\\\"receive eagle_log_1 log, id:[{}]\\\", $log.getId());\\n    String type = $log.getType();\\n    if (\\\"opm\\\".equals(type)){\\n        String logTime = RegexUtil.extractString(\\\"(\\\\\\\\d{4}-\\\\\\\\d{2}-\\\\\\\\d{2} \\\\\\\\d{2}:\\\\\\\\d{2}:\\\\\\\\d{2}.\\\\\\\\d{3})\\\", $msg);\\n        if(logTime == null){\\n            LOG.warn(\\\"invalid date or time, log: {}\\\", $msg);\\n            return;\\n        }\\n        Date date = DateUtil.convertFromString(\\\"yyyy-MM-dd HH:mm:ss.SSS\\\", logTime);\\n        $log.setTimestamp(date != null ? date : $log.getAtTimestamp());\\n        if ($msg.contains(\\\"EventTracking\\\")){\\n            String tracking = RegexUtil.extractString(\\\"(EventTracking.+)\\\", $msg);\\n            if (tracking != null){\\n                String[]  tracks = tracking.split(\\\"\\\\\\\\|\\\");\\n                $log.addField(\\\"EventType\\\",tracks[0]);\\n                $log.addField(\\\"LogType\\\",tracks[1]);\\n                $log.addField(\\\"LogId\\\",tracks[2]);\\n                $log.addField(\\\"UserId\\\",tracks[3]);\\n                $log.addField(\\\"LogTime\\\",tracks[4]);\\n            }\\n        }\\n    } else if (\\\"offermanager\\\".equals(type)){\\n            String logTime = RegexUtil.extractString(\\\"(\\\\\\\\d{4}-\\\\\\\\d{2}-\\\\\\\\d{2} \\\\\\\\d{2}:\\\\\\\\d{2}:\\\\\\\\d{2},\\\\\\\\d{3})\\\", $msg);\\n            if(logTime == null){\\n                LOG.warn(\\\"invalid date or time, log: {}\\\", $msg);\\n                return;\\n            }\\n            Date date = DateUtil.convertFromString(\\\"yyyy-MM-dd HH:mm:ss,SSS\\\", logTime);\\n            $log.setTimestamp(date != null ? date : $log.getAtTimestamp());\\n        } else {\\n            return;\\n        }\\n\\n    long delayTime = (System.currentTimeMillis() - $log.getTimestamp().getTime())/1000;\\n    if (delayTime > 5*24*3600 || delayTime < -5*24*3600) {\\n        LOG.warn(\\\"Too early or too late log, ignore it, delay:{}, log:{}\\\", delayTime, $log.getTimestamp().getTime());\\n        return;\\n    }\\n    $log.dealDone();\\n    LOG.debug(\\\"out -----eagle_log_1------\\\");\\nend\",\n" +
            "\t\"version\": \"20190729\",\n" +
            "\t\"state\": 1,\n" +
            "\t\"updateTime\": 1564475611452\n" +
            "}, {\n" +
            "\t\"id\": 456,\n" +
            "\t\"appId\": \"2a2df323-d2ea-45ca-bf7e-6d2afa125688\",\n" +
            "\t\"type\": \"logrules\",\n" +
            "\t\"script\": \"package logrules\\n\\nimport org.slf4j.Logger;\\nimport java.util.Date;\\nimport java.util.Locale;\\nimport com.alarm.eagle.log.LogEntry;\\nimport com.alarm.eagle.util.RegexUtil;\\nimport com.alarm.eagle.util.DateUtil\\nimport java.time.LocalDateTime;\\n\\nglobal Logger LOG;\\n\\nrule \\\"eagle_log_2 Log Rule\\\"\\n\\tno-loop true\\n\\tsalience 100\\n    when\\n        $log : LogEntry(index == \\\"eagle_log_2\\\", $msg : message)\\n    then\\n        LOG.debug(\\\"receive eagle_log_2 log, id:[{}]\\\", $log.getId());\\n        String logTime = RegexUtil.extractString(\\\"(\\\\\\\\d{4}-\\\\\\\\d{2}-\\\\\\\\d{2} \\\\\\\\d{2}:\\\\\\\\d{2}:\\\\\\\\d{2}.\\\\\\\\d{3})\\\", $msg);\\n        if(logTime == null){\\n            LOG.warn(\\\"invalid date or time, log: {}\\\", $msg);\\n            return;\\n        }\\n        Date date = DateUtil.convertFromString(\\\"yyyy-MM-dd HH:mm:ss,SSS\\\", logTime);\\n        $log.setTimestamp(date!=null? date:$log.getAtTimestamp());\\n\\n        $log.dealDone();\\n        LOG.debug(\\\"out ----- eagle_log_2 log-----\\\");\\nend\",\n" +
            "\t\"version\": \"20190826\",\n" +
            "\t\"state\": 1,\n" +
            "\t\"updateTime\": 1567078969483\n" +
            "}]";

    public static String doGetMock(String urlPath) {
        return mockRules;
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
