package com.alarm.eagle.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;

public class JsonUtil {
    private static Logger logger = LoggerFactory.getLogger(JsonUtil.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES , false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS,false);
    }

    public static ObjectNode createObjectNode() {
        return mapper.createObjectNode();
    }

    public static <T> T convertValue(Object o, Class<T> valueType) {
        return mapper.convertValue(o, valueType);
    }

    public static <T> T decode(String jsonStr, Class<T> valueType){
        try {
            return mapper.readValue(jsonStr, valueType);
        }catch (Exception e) {
            logger.error("jsonStr={}||valueType={}||error=", jsonStr, valueType, e);
            return null;
        }
    }

    public static <T> T decode(String jsonStr, TypeReference valueTypeRef){
        try {
            return mapper.readValue(jsonStr, valueTypeRef);
        }catch (Exception e) {
            logger.error("jsonStr={}||valueTypeRef={}||error=", jsonStr, valueTypeRef, e);
            return null;
        }
    }

    public static String encode(Object o) {
        try {
            return mapper.writeValueAsString(o);
        }catch (Exception e) {
            logger.error("object={}||error=", o, e);
            return null;
        }
    }

    public static <T> byte[] serialize(T obj) {
        byte[] bytes = new byte[0];
        try {
            bytes = mapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return bytes;
    }

    public static <T> T deserialize(byte[] data, Class<T> cls) {
        T obj = null;
        try {
            obj = mapper.readValue(data, cls);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return obj;
    }

    public static String objectToJson(Object o) {
        String json = "";
        try {
            json = mapper.writeValueAsString(o);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return json;
    }

    public static <type> type jsonToObject(String json, Class<?> cls) {
        type obj = null;
        JavaType javaType = mapper.getTypeFactory().constructType(cls);
        try {
            obj = mapper.readValue(json, javaType);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return obj;
    }

    public static <type> type jsonToObjectList(String json,
                                               Class<?> collectionClass, Class<?>... elementClass) {
        type obj = null;
        JavaType javaType = mapper.getTypeFactory().constructParametricType(
                collectionClass, elementClass);
        try {
            obj = mapper.readValue(json, javaType);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return obj;
    }

    public static <type> type jsonToObjectHashMap(String json,
                                                  Class<?> keyClass, Class<?> valueClass) {
        type obj = null;
        JavaType javaType = mapper.getTypeFactory().constructParametricType(HashMap.class, keyClass, valueClass);
        try {
            obj = mapper.readValue(json, javaType);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return obj;
    }

}
