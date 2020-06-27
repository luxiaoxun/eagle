package com.alarm.eagle.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringUtil {
    private static final Logger logger = LoggerFactory.getLogger(StringUtil.class);

    public static String getFirstLine(String msg) {
        return getLineAt(msg, 0);
    }

    public static String getStartLine(String msg) {
        String[] lines = getLines(msg);
        if (lines != null) {
            for (String line : lines) {
                if (line.startsWith("[start"))
                    return line;
            }
        }
        return null;
    }

    public static String getLineAt(String msg, int index) {
        String[] lines = getLines(msg);
        return lines != null && index >= 0 && index < lines.length
                ? lines[index] : null;
    }

    public static String[] getLines(String msg) {
        return msg.split("[\r\n]+");
    }

    public static String removeAllBlank(String msg) {
        return msg.replaceAll("\\b", "");
    }

    public static String getSubString(String msg, String start, String end) {
        int beginIndex = msg.indexOf(start);
        if (beginIndex == -1) {
            return null;
        }
        beginIndex = beginIndex + start.length();
        int endIndex = msg.indexOf(end, beginIndex);
        if (endIndex == -1) {
            return null;
        }
        return msg.substring(beginIndex, endIndex);
    }
}
