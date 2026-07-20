package com.alarm.eagle.cep.json.spec;

import org.apache.flink.streaming.api.windowing.time.Time;

/**
 * Times constraint. Corresponds to {@code Pattern.times(from, to)} and
 * {@code Pattern.timesOrMore(from).within(windowTime)} in Flink CEP.
 */
public class TimesSpec {

    private final int from;
    private final int to;
    /** Time window within the loop; nullable */
    private final Time windowTime;

    public TimesSpec() {
        this(0, 0, null);
    }

    public TimesSpec(int from, int to, Time windowTime) {
        this.from = from;
        this.to = to;
        this.windowTime = windowTime;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public Time getWindowTime() {
        return windowTime;
    }

    @Override
    public String toString() {
        return "TimesSpec{from=" + from + ", to=" + to + ", windowTime=" + windowTime + '}';
    }
}
