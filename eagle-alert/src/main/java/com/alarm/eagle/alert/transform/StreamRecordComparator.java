package com.alarm.eagle.alert.transform;

import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;

import java.util.Comparator;

/**
 * Created by skycrab on 18/1/8.
 */
public class StreamRecordComparator implements Comparator<StreamRecord> {
    @Override
    public int compare(StreamRecord o1, StreamRecord o2) {
        if (o1.getTimestamp() < o2.getTimestamp()) {
            return -1;
        } else if (o1.getTimestamp() > o2.getTimestamp()) {
            return 1;
        } else {
            return 0;
        }
    }
}
