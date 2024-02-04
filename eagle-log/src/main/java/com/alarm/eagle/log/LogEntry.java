package com.alarm.eagle.log;

import com.alarm.eagle.constant.Constant;
import com.alarm.eagle.util.DateUtil;
import com.alarm.eagle.util.Md5Util;

import com.alarm.eagle.util.StringUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

    //每条日志必须要有的字段
    private Date timestamp;
    private Date atTimestamp;
    private String ip;
    private String message;
    private String path;
    private long offset = -1;
    private boolean wrongLog = true;
    private boolean jsonLog = false;
    private String jsonStr = null;

    private Map<String, Object> fields = new HashMap<>();

    public LogEntry() {
        atTimestamp = new Date();
    }

    public LogEntry(JsonObject json) {
        jsonStr = json.toString();

        if (json.has("ip")) {
            ip = json.get("ip").getAsString();
        }
        if (json.has("index")) {
            index = json.get("index").getAsString();
        }
        if (json.has("message")) {
            message = json.get("message").getAsString();
        }
        if (json.has("type")) {
            type = json.get("type").getAsString();
        }
        if (json.has("@timestamp")) {
            atTimestamp = DateUtil.toAtTimestampWithZone(json.get("@timestamp").getAsString());
        }
        if (json.has("source")) {
            path = json.get("source").getAsString();
        }
        if (json.has("timestamp")) {
            timestamp = DateUtil.toTimestamp(json.get("timestamp").getAsString());
        }

        if (json.has("offset")) {
            offset = json.get("offset").getAsLong();
        }

        if (message != null) {
            id = generateId();
        } else {
            id = Md5Util.getMd5(json.toString());
        }

        if (json.has("indexTarget")) {
            indexTarget = json.get("indexTarget").getAsString();
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

    private void setJson(JsonObject json, String key, Object value) {
        if (!StringUtil.isEmpty(key) && value != null) {
            json.add(key, new Gson().toJsonTree(value));
        }
    }

    public boolean isJsonLog() {
        return jsonLog;
    }

    public void setJsonLog(boolean jsonLog) {
        this.jsonLog = jsonLog;
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
        JsonObject jsonObj = (JsonObject) JsonParser.parseString(this.jsonStr);
        setJson(jsonObj, key, value);
        this.jsonStr = jsonObj.toString();
    }

    public JsonObject getJson() {
        JsonObject jsonObj = (JsonObject) JsonParser.parseString(this.jsonStr);
        return jsonObj;
    }

    public JsonObject toJSON() {
        if (jsonLog) {
            this.setJson("index", index);
            JsonObject jsonObj = (JsonObject) JsonParser.parseString(this.jsonStr);
            return jsonObj;
        }

        JsonObject json = new JsonObject();
        json.addProperty("ip", ip);
        json.addProperty("index", index);
        json.addProperty("message", message);
        json.addProperty("path", path);
        json.addProperty("source", path);

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
                json.add(entry.getKey(), new Gson().toJsonTree(entry.getValue()));
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

    public boolean isWrongLog() {
        return wrongLog;
    }

    public void setWrongLog(boolean wrongLog) {
        this.wrongLog = wrongLog;
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

        if (Constant.PATTERN_WEEK.equals(esIndexPostfix)) {
            return String.format("%s%s-v%s", index, esIndexPrefix, date);
        }

        return String.format("%s%s-%s", index, esIndexPrefix, date);
    }

    public void handleError() {
        this.jsonLog = false;
        if (this.ip == null) {
            this.ip = "0.0.0.0";
        }
        if (this.id == null) {
            this.id = Md5Util.getMd5(this.getJson().toString());
        }
        this.addField("index", this.getIndex());
        this.setIndex(Constant.WRONG_LOG_INDEX);
        this.setTimestamp(this.getAtTimestamp() != null ? this.getAtTimestamp() : this.getTimestamp());
        this.setMessage(this.getJson().toString());
        this.setWrongLog(false);
    }

    public void dealDone() {
        calculateDelayTime();
        setWrongLog(false);
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
                + fields + ", wrongLog=" + wrongLog + "]";
    }
}
