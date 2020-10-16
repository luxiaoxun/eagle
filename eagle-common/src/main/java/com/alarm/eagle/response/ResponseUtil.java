package com.alarm.eagle.response;

public class ResponseUtil {
    public ResponseUtil() {
    }

    public static Response success() {
        return generateResult(ResponseCode.SUCCESS, (Object)null);
    }

    public static Response success(Object data) {
        return generateResult(ResponseCode.SUCCESS, data);
    }

    public static Response fail() {
        return generateResult(ResponseCode.SERVER_ERROR, (Object)null);
    }

    public static Response fail(int code, String msg) {
        return generateResult(code, msg, (Object)null);
    }

    public static Response generateResult(int code, String msg, Object data) {
        return new Response(code, msg, data);
    }

    public static Response generateResult(ResponseCode responseCode, Object data) {
        return generateResult(responseCode.getCode(), responseCode.getMsg(), data);
    }
}
