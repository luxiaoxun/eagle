package com.alarm.eagle.util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtil {
    private static final Logger logger = LoggerFactory.getLogger(DateUtil.class);

    private static final DateTimeFormatter YMD_HMS = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter zFORMAT = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final DateTimeFormatter FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public static long unixTimestamp(String ymdhms) {
        return YMD_HMS.parseMillis(ymdhms);
    }

    public static long unixTimestamp(Date date) {
        DateTime dateTime = new DateTime(date);
        return dateTime.getMillis();
    }

    public static String fromUnixtime(long timestamp) {
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

    public static Date getNowDateMinute() {
        DateTime d = DateTime.now();
        d.withSecondOfMinute(0);
        return d.toDate();
    }

    public static Date toAtTimestampWithZone(String dataStr) {
        return zFORMAT.parseDateTime(dataStr).toDate();
    }

    public static Date toAtTimestamp(String dataStr) {
        return FORMAT.parseDateTime(dataStr).toDate();
    }

    public static String convertToUTCString(String format, long timestamp) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat.format(timestamp);
    }

    public static String convertToLocalString(String format, long timestamp) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        simpleDateFormat.setTimeZone(TimeZone.getDefault());
        return simpleDateFormat.format(timestamp);
    }

    public static String getEsString(long timestamp) {
        return convertToLocalString("yyyy-MM-dd'T'HH:mm:ss.SSSZ", timestamp);
    }

    public static Date convertFromString(String format, String strDate, Locale locale) {
        if (format.indexOf("y") < 0) {
            // 没有年份，添加当前时间的年
            SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
            String year = yearFormat.format(System.currentTimeMillis());
            strDate = year + " " + strDate;
            format = "yyyy" + " " + format;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format, locale);
        try {
            return sdf.parse(strDate);
        } catch (ParseException e) {
        }
        return null;
    }

    public static Date convertFromString(String format, String strDate) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            return sdf.parse(strDate);
        } catch (ParseException e) {
        }
        return null;
    }

    public static long parseTimestamp(String msg) {
        String[] items = msg.replaceAll("\\.", ":").split(":", 4);
        long time = 0;
        try {
            if (items.length >= 3) {
                time = Integer.parseInt(items[0].trim()) * 3600000 + Integer.parseInt(items[1].trim()) * 60000
                        + Integer.parseInt(items[2].trim()) * 1000;
            }
            if (items.length == 4) {
                time += Integer.parseInt(items[3].trim());
            }
        } catch (Exception e) {
            logger.error("Faild to parse time:{}", msg);
        }

        return time;
    }

    public static long tomorrowZeroTimestampMs(long now, int timeZone) {
        return now - (now + timeZone * 3600000) % 86400000 + 86400000;
    }

    public static void main(String[] args) {
        System.out.println(getEsString(tomorrowZeroTimestampMs(System.currentTimeMillis(), 8)));
    }

}
