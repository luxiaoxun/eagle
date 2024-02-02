package com.alarm.eagle.policy.transform;

import com.alarm.eagle.model.Field;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by skycrab on 17/12/25.
 */
public class NodeToRowMapFunc implements MapFunction<ObjectNode, Row> {

    private static Logger logger = LoggerFactory.getLogger(NodeToRowMapFunc.class);

    private List<Field> fieldList;

    public NodeToRowMapFunc(List<Field> fieldList) {
        this.fieldList = fieldList;
    }

    @Override
    public Row map(ObjectNode node) throws Exception {
        Row row = new Row(fieldList.size());
        for (int i = 0; i < fieldList.size(); i++) {
            Field field = fieldList.get(i);
            String key = field.getFieldName();
            Object value = null;
            switch (field.getFieldType()) {
                case "int":
                    value = node.get(key).asInt();
                    break;
                case "long":
                    value = node.get(key).asLong();
                    break;
                case "double":
                    value = node.get(key).asDouble();
                    break;
                case "boolean":
                    value = node.get(key).asBoolean();
                    break;
                case "string":
                    value = node.get(key).asText();
                    break;
            }
            row.setField(i, value);
        }
        return row;
    }
}
