package com.alarm.eagle.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Md5Util {
    public static String MD5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100), 1, 3);
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }
}
