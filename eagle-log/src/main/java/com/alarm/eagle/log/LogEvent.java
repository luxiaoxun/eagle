package com.alarm.eagle.log;

import com.alarm.eagle.constant.Constant;
import com.alarm.eagle.util.DateUtil;
import com.alarm.eagle.util.JsonUtil;
import com.alarm.eagle.util.Md5Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by luxiaoxun on 2020/01/27.
 */
public class LogEvent extends HashMap<String, Object> {
    private static final Logger logger = LoggerFactory.getLogger(LogEvent.class);

    public LogEvent() {
        Date now = new Date();
        put(Constant.AT_TIMESTAMP, DateUtil.toUtcTimestamp(now.getTime()));
        put(Constant.TIMESTAMP, DateUtil.toUtcTimestamp(now.getTime()));
        put(Constant.MESSAGE, "");
        String id = generateId();
        put(Constant.ID, id);
    }

    public LogEvent(Map map) {
        super(map);
        String id = generateId();
        put(Constant.ID, id);
    }

    @Override
    public Object put(String key, Object value) {
        if (value == null) {
            return null;
        }
        return super.put(key, value);
    }

    private String generateId() {
        return Md5Util.MD5(getMessage());
    }

    public String getId() {
        return get(Constant.ID).toString();
    }

    public void setId(String id) {
        put(Constant.ID, id);
    }

    public String getIp() {
        return get(Constant.IP).toString();
    }

    public void setIp(String ip) {
        put(Constant.IP, ip);
    }

    public String getIndex() {
        return get(Constant.INDEX).toString();
    }

    public void setIndex(String index) {
        put(Constant.INDEX, index);
    }

    public String getType() {
        return get(Constant.TYPE).toString();
    }

    public void setType(String type) {
        put(Constant.TYPE, type);
    }

    public String getMessage() {
        return get(Constant.MESSAGE).toString();
    }

    public void setMessage(String message) {
        put(Constant.MESSAGE, message);
        String newId = generateId();
        put(Constant.ID, newId);
    }

    public String getPath() {
        return get(Constant.PATH).toString();
    }

    public void setPath(String path) {
        put(Constant.PATH, path);
    }

    public Date getTimestamp() {
        String getTimestamp = get(Constant.TIMESTAMP).toString();
        return DateUtil.toTimestamp(getTimestamp);
    }

    public void setTimestamp(Date timestamp) {
        put(Constant.TIMESTAMP, DateUtil.toUtcTimestamp(timestamp.getTime()));
    }

    public Date getAtTimestamp() {
        String getAtTimestamp = get(Constant.AT_TIMESTAMP).toString();
        return DateUtil.toTimestamp(getAtTimestamp);
    }

    public void setAtTimestamp(Date atTimestamp) {
        put(Constant.AT_TIMESTAMP, DateUtil.toUtcTimestamp(atTimestamp.getTime()));
    }

    public String getIndexTarget() {
        return get(Constant.INDEX_TARGET).toString();
    }

    public void setIndexTarget(String indexTarget) {
        put(Constant.INDEX_TARGET, indexTarget);
    }

    public boolean isErrorLog() {
        return getBooleanVal(Constant.IS_ERROR_LOG, false);
    }

    public void setErrorLog(boolean errorLog) {
        put(Constant.IS_ERROR_LOG, errorLog);
    }

    public LogEvent addField(String key, Object value) {
        put(key, value);
        return this;
    }

    public Object getField(String key) {
        Object obj = this.get(key);
        return key != null ? obj : "";
    }


    public void setIndexTimeFormat(String indexTimeFormat) {
        put(Constant.INDEX_TIME_FORMAT, indexTimeFormat);
    }

    public String getIndexTimeFormat() {
        return getOrDefault(Constant.INDEX_TIME_FORMAT, Constant.PATTERN_DAY).toString();
    }

    public void setIndexPostfix(String indexPostfix) {
        put(Constant.INDEX_POSTFIX, indexPostfix);
    }

    public String getIndexPostfix() {
        return getOrDefault(Constant.INDEX_POSTFIX, "").toString();
    }

    public String generateIndexName() {
        String date;
        String indexTimeFormat = getIndexTimeFormat();
        if (getTimestamp() != null) {
            date = DateUtil.convertToLocalString(indexTimeFormat, getTimestamp().getTime());
        } else if (getAtTimestamp() != null) {
            date = DateUtil.convertToLocalString(indexTimeFormat, getAtTimestamp().getTime());
        } else {
            logger.warn("Use current date time for log id:{}", getId());
            date = DateUtil.convertToLocalString(indexTimeFormat, new Date().getTime());
        }

        String index = getIndex();
        String indexPostfix = getIndexPostfix();
        if (Constant.PATTERN_WEEK.equals(indexTimeFormat)) {
            return String.format("eagle_log_%s%s-v%s", index, indexPostfix, date);
        }

        return String.format("eagle_log_%s%s-%s", index, indexPostfix, date);
    }

    public void handleError() {
        this.addField(Constant.INDEX_SOURCE, getIndex());
        this.setIndex(Constant.WRONG_LOG_INDEX);
        this.setTimestamp(this.getAtTimestamp() != null ? this.getAtTimestamp() : this.getTimestamp());
        this.setErrorLog(false);
    }

    public void dealDone() {
        calculateDelayTime();
        setErrorLog(false);
    }

    public void calculateDelayTime() {
        Date timestamp = getTimestamp();
        Date atTimestamp = getAtTimestamp();
        if (timestamp == null || atTimestamp == null) {
            return;
        }
        addField("delay_time", atTimestamp.getTime() - timestamp.getTime());
    }

    public boolean getBooleanVal(String key, boolean defaultValue) {
        try {
            Object value = get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof String) {
                return Boolean.parseBoolean(value.toString());
            } else {
                return defaultValue;
            }
        } catch (Exception e) {
            logger.error("{}", e);
            return defaultValue;
        }
    }

    @Override
    public String toString() {
        return JsonUtil.encode(this);
    }
}
