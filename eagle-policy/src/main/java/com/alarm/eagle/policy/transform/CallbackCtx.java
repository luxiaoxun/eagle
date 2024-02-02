package com.alarm.eagle.policy.transform;

import com.alarm.eagle.model.DataSink;
import org.apache.flink.streaming.api.operators.Output;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.wso2.siddhi.query.api.definition.Attribute;

import java.io.Serializable;
import java.util.List;

/**
 * Created by skycrab on 18/1/8.
 */
public class CallbackCtx implements Serializable {
    private DataSink dataSink;

    private List<Attribute> attributeList;

    private Output<StreamRecord<DataSink>> output;


    public CallbackCtx(DataSink dataSink, List<Attribute> attributeList, Output<StreamRecord<DataSink>> output) {
        this.dataSink = dataSink;
        this.attributeList = attributeList;
        this.output = output;
    }

    public DataSink getDataSink() {
        return dataSink;
    }

    public List<Attribute> getAttributeList() {
        return attributeList;
    }

    public Output<StreamRecord<DataSink>> getOutput() {
        return output;
    }
}
