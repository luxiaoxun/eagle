package com.alarm.eagle.policy.source;

import com.alarm.eagle.util.JsonUtil;

import java.io.Serializable;

/**
 * Created by skycrab on 17/12/22.
 */
public class Event implements Serializable {
    /**
     * 时间错
     */
    private String timestamp;
    /**
     * 温度
     */
    private double temperature;
    /**
     * 机架
     */
    private int rack;

    public Event(String timestamp, double temperature, int rack) {
        this.timestamp = timestamp;
        this.temperature = temperature;
        this.rack = rack;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getRack() {
        return rack;
    }

    public void setRack(int rack) {
        this.rack = rack;
    }


    @Override
    public String toString() {
        return JsonUtil.encode(this);
    }

}
