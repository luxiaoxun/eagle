package com.alarm.eagle.api.bean;

import com.alarm.eagle.util.JsonUtil;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.io.Serializable;

/**
 * 定义字段名称，以及类型
 * Created by luxiaoxun on 17/12/25.
 */
public class Field implements Serializable{

    public Field() {

    }

    public Field(String fieldName, String fieldType) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    /**
     * 字段名称
     */
    private String fieldName;

    /**
     * 字段类型
     */
    private String fieldType;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    @Override
    public String toString() {
        return JsonUtil.encode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
