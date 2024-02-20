package com.alarm.eagle.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtil {
    private static final Logger logger = LoggerFactory.getLogger(DateUtil.class);

    private static final DateTimeFormatter YMD_HMS = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter YMD_HMS_SSS = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter zFORMAT = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final DateTimeFormatter FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public static long unixTimestamp(String ymdhms) {
        return YMD_HMS.parseMillis(ymdhms);
    }

    public static long unixTimestamp(Date date) {
        DateTime dateTime = new DateTime(date);
        return dateTime.getMillis();
    }

    public static String toUnixTimeString(long timestamp) {
        return YMD_HMS.print(timestamp);
    }

    public static DateTime toDate(String ymdhms) {
        DateTime d = YMD_HMS.parseDateTime(ymdhms);
        return d;
    }

    public static int hour(String ymdhms) {
        DateTime d = toDate(ymdhms);
        return d.getHourOfDay();
    }

    public static int hour() {
        DateTime d = DateTime.now();
        return d.getHourOfDay();
    }

    public static int year() {
        DateTime d = DateTime.now();
        return d.getYear();
    }

    public static Date getNowDateMinute() {
        DateTime d = DateTime.now();
        d.withSecondOfMinute(0);
        return d.toDate();
    }

    public static String toUtcTimestamp(long timestamp) {
        return YMD_HMS_SSS.withZoneUTC().print(timestamp);
    }

    public static Date toAtTimestampWithZone(String dataStr) {
        return zFORMAT.parseDateTime(dataStr).toDate();
    }

    public static Date toTimestamp(String dataStr) {
        return YMD_HMS_SSS.parseDateTime(dataStr).toDate();
    }

    public static String convertToUTCString(String format, long timestamp) {
        return DateTimeFormat.forPattern(format).withZoneUTC().print(timestamp);
    }

    public static String convertToLocalString(String format, long timestamp) {
        return DateTimeFormat.forPattern(format).withZone(DateTimeZone.forTimeZone(TimeZone.getDefault())).print(timestamp);
    }

    public static String getEsString(long timestamp) {
        return convertToLocalString("yyyy-MM-dd'T'HH:mm:ss.SSSZ", timestamp);
    }

    public static Date convertFromString(String format, String strDate, Locale locale) {
        if (format.indexOf("y") < 0) {
            // 没有年份，添加当前时间的年
            int year = DateTime.now().getYear();
            strDate = year + " " + strDate;
            format = "yyyy" + " " + format;
        }
        return DateTimeFormat.forPattern(DateTimeFormat.patternForStyle(format, locale)).parseDateTime(strDate).toDate();
    }

    public static Date convertFromString(String format, String strDate) {
        return DateTimeFormat.forPattern(format).parseDateTime(strDate).toDate();
    }

    public static long tomorrowZeroTimestampMs(long now, int timeZone) {
        return now - (now + timeZone * 3600000L) % 86400000 + 86400000;
    }

}
