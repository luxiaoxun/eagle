package com.alarm.eagle.log;

import com.alarm.eagle.util.DateUtil;
import com.alarm.eagle.util.Md5Util;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by luxiaoxun on 2020/01/27.
 */
public class LogEntry implements Serializable {
    private static final long serialVersionUID = -2557354597999638954L;

    private static final Logger logger = LoggerFactory.getLogger(LogEntry.class);

    private String id;
    private String type = null;
    private String index;
    private String indexTarget = "";
    private String esIndexPrefix = "";
    private String esIndexPostfix = "yyyy-MM-dd";

    private final static String PATTERN_DAY = "yyyy-MM-dd";  //天
    private final static String PATTERN_WEEK = "YYYY-ww";    //周
    private final static String PATTERN_MONTH = "yyyy-MM";   //月
    private final static String PATTERN_YEAR = "yyyy";       //年

    //每条日志必须要有的字段
    private Date timestamp;
    private Date atTimestamp;
    private String ip;
    private String message;
    private String path;
    private long offset = -1;

    private boolean filter = true;
    private boolean isJsonLog = false;
    private String jsonStr = null;

    private Map<String, Object> fields = new HashMap<>();

    public LogEntry() {
        atTimestamp = new Date();
    }

    public LogEntry(JSONObject json) {
        jsonStr = json.toJSONString();

        if (json.containsKey("ip")) {
            ip = json.getString("ip");
        }
        if (json.containsKey("index")) {
            index = json.getString("index");
        }
        if (json.containsKey("message")) {
            message = json.getString("message");
        }
        if (json.containsKey("type")) {
            type = json.getString("type");
        }
        if (json.containsKey("@timestamp")) {
            atTimestamp = DateUtil.toAtTimestampWithZone(json.getString("@timestamp"));
        }
        if (json.containsKey("source")) {
            path = json.getString("source");
        }
        if (json.containsKey("timestamp")) {
            timestamp = DateUtil.toAtTimestampWithZone(json.getString("timestamp"));
        }

        if (json.containsKey("offset")) {
            offset = json.getLongValue("offset");
        }

        if (message != null) {
            id = generateId();
        } else {
            id = Md5Util.getMd5(json.toString());
        }

        if (json.containsKey("indexTarget")) {
            indexTarget = json.getString("indexTarget");
        }

    }

    private String generateId() {
        String id = Md5Util.getMd5(message + ip + index + path + offset);
        return id;
    }

    public LogEntry duplicate() {
        try {
            return (LogEntry) this.clone();
        } catch (CloneNotSupportedException e) {
            logger.error("Exception occurs:", e);
        }
        return null;
    }

    public String getIndexTarget() {
        return indexTarget;
    }

    public void setIndexTarget(String indexTarget) {
        this.indexTarget = indexTarget;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
        this.id = generateId();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Date getAtTimestamp() {
        return atTimestamp;
    }

    public void setAtTimestamp(Date atTimestamp) {
        this.atTimestamp = atTimestamp;
    }

    public LogEntry addField(String key, Object value) {
        if (value != null) {
            fields.put(key, value);
        }
        return this;
    }

    public Object getField(String key) {
        Object obj = fields.get(key);
        return key != null ? obj : "";
    }

    private void setJson(JSONObject json, String key, Object value) {
        if (value != null) {
            json.put(key, value);
        }
    }

    public void enableJsonLog() {
        isJsonLog = true;
    }

    public boolean isJsonLog() {
        return isJsonLog;
    }

    public void setIsJsonLog(boolean jsonLog) {
        isJsonLog = jsonLog;
    }

    public String getJsonStr() {
        return jsonStr;
    }

    public void setJsonStr(String jsonStr) {
        this.jsonStr = jsonStr;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }

    public void setJson(String key, Object value) {
        JSONObject jsonObj = (JSONObject) JSONObject.parse(this.jsonStr);
        setJson(jsonObj, key, value);
        this.jsonStr = jsonObj.toJSONString();
    }

    public JSONObject getJson() {
        JSONObject jsonObj = (JSONObject) JSONObject.parse(this.jsonStr);
        return jsonObj;
    }

    public JSONObject toJSON() {
        if (isJsonLog) {
            this.setJson("index", index);
            JSONObject jsonObj = (JSONObject) JSONObject.parse(this.jsonStr);
            return jsonObj;
        }

        JSONObject json = new JSONObject();
        json.put("ip", ip);
        setJson(json, "index", index);
        setJson(json, "message", message);
        setJson(json, "path", path);
        setJson(json, "source", path);

        if (atTimestamp != null) {
            setJson(json, "attimestamp", DateUtil.getEsString(atTimestamp.getTime()));
        }

        if (type != null) {
            setJson(json, "type", type);
        }

        if (timestamp != null) {
            setJson(json, "timestamp", DateUtil.getEsString(timestamp.getTime()));
        }

        if (offset >= 0) {
            setJson(json, "offset", offset);
        }

        if (!fields.isEmpty()) {
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
        }
        return json;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getOffset() {
        return offset;
    }

    public boolean isFilter() {
        return filter;
    }

    public void setFilter(boolean filter) {
        this.filter = filter;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEsIndexPostfix() {
        return esIndexPostfix;
    }

    public void setEsIndexPostfix(String esIndexPostfix) {
        this.esIndexPostfix = esIndexPostfix;
    }

    public String getEsIndexPrefix() {
        return esIndexPrefix;
    }

    public void setEsIndexPrefix(String esIndexPrefix) {
        this.esIndexPrefix = esIndexPrefix;
    }

    public String generateIndexName() {
        String date;
        if (getTimestamp() != null) {
            date = DateUtil.convertToLocalString(esIndexPostfix, getTimestamp().getTime());
        } else if (getAtTimestamp() != null) {
            date = DateUtil.convertToLocalString(esIndexPostfix, getAtTimestamp().getTime());
        } else {
            logger.error("Invalid log was inserted: {}", getMessage());
            date = DateUtil.convertToLocalString(esIndexPostfix, new Date().getTime());
        }

        if (PATTERN_WEEK.equals(esIndexPostfix)) {
            return String.format("%s%s-v%s", index, esIndexPrefix, date);
        }

        return String.format("%s%s-%s", index, esIndexPrefix, date);
    }

    public void handleError() {
        this.isJsonLog = false;
        if (this.ip == null) {
            this.ip = "0.0.0.0";
        }
        if (this.id == null) {
            this.id = Md5Util.getMd5(this.getJson().toString());
        }
        this.addField("index", this.getIndex());
        this.setIndex("error_log");
        this.setTimestamp(this.getAtTimestamp() != null ? this.getAtTimestamp() : this.getTimestamp());
        this.setMessage(this.getJson().toString());
        this.setFilter(false);
    }

    public void dealDone() {
        calculateDelayTime();
        setFilter(false);
    }

    public void calculateDelayTime() {
        if (timestamp == null || atTimestamp == null) {
            return;
        }
        addField("delaytime", atTimestamp.getTime() - timestamp.getTime());
    }

    @Override
    public String toString() {
        return "LogEntry [id=" + id + ", type=" + type + ", index=" + index + ", timestamp=" + timestamp
                + ", atTimestamp=" + atTimestamp + ", ip=" + ip + ", message=" + message + ", path=" + path + ", fields="
                + fields + ", filter=" + filter + "]";
    }
}
