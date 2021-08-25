package com.alarm.eagle.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtil {
    public static String[] extractStrings(String pattern, String msg) {
        Matcher matcher = Pattern.compile(pattern).matcher(msg);
        List<String> result = new ArrayList<>();
        if (matcher.find()) {
            int len = matcher.groupCount();
            for (int i = 1; i <= len; ++i) {
                String a = matcher.group(i);
                result.add(a);
            }
        }
        return result.toArray(new String[0]);
    }

    public static String[] extractAllStrings(String pattern, String msg) {
        Matcher matcher = Pattern.compile(pattern).matcher(msg);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result.toArray(new String[0]);
    }

    public static String extractString(String pattern, String msg, int index) {
        try {
            Matcher matcher = Pattern.compile(pattern).matcher(msg);
            if (index <= matcher.groupCount() && matcher.find()) {
                return matcher.group(index);
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static String extractString(String pattern, String msg) {
        return extractString(pattern, msg, 1);
    }

    public static List<Pair<String, String>> extractPairs(String pattern, String msg) {
        List<Pair<String, String>> result = new ArrayList<>();
        Matcher matcher = Pattern.compile(pattern).matcher(msg);
        if (matcher.groupCount() >= 2) {
            while (matcher.find()) {
                result.add(Pair.of(matcher.group(1), matcher.group(2)));
            }
        }
        return result;
    }

    public static Map<String, String> extractMap(String pattern, String msg) {
        Map<String, String> result = new HashMap<>();
        Matcher matcher = Pattern.compile(pattern).matcher(msg);
        if (matcher.groupCount() >= 2) {
            while (matcher.find()) {
                result.put(matcher.group(1), matcher.group(2));
            }
        }
        return result;
    }

    public static boolean isMatch(String pattern, String line) {
        Matcher matcher = Pattern.compile(pattern).matcher(line);
        return matcher.find();
    }

}
