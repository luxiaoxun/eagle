package com.alarm.eagle.response;

public class ResponseUtil {
    public ResponseUtil() {
    }

    public static Response success() {
        return generateResult(ResponseCode.SUCCESS, (Object) null);
    }

    public static Response success(Object data) {
        return generateResult(ResponseCode.SUCCESS, data);
    }

    public static Response fail() {
        return generateResult(ResponseCode.SYSTEM_ERROR, (Object) null);
    }

    public static Response fail(String msg) {
        return generateResult(ResponseCode.SYSTEM_ERROR, msg, (Object) null);
    }

    public static Response fail(ResponseCode code, String msg) {
        return generateResult(code, msg, (Object) null);
    }

    public static Response generateResult(ResponseCode code, String msg, Object data) {
        return new Response(code.getCode(), msg, data);
    }

    public static Response generateResult(ResponseCode responseCode, Object data) {
        return generateResult(responseCode, responseCode.getMsg(), data);
    }
}
