package com.alarm.eagle.message;

public interface TimestampAssignable<T> {
    void assignIngestionTimestamp(T timestamp);
}
