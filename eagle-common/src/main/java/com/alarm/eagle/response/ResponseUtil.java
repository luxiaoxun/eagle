package com.alarm.eagle.response;

import com.alarm.eagle.enumerate.CodeEnum;

public class ResponseUtil {
    public ResponseUtil() {
    }

    public static Response success() {
        return generateResult(CodeEnum.SUCCESS, (Object)null);
    }

    public static Response success(Object data) {
        return generateResult(CodeEnum.SUCCESS, data);
    }

    public static Response fail() {
        return generateResult(CodeEnum.SERVER_ERROR, (Object)null);
    }

    public static Response fail(int code, String msg) {
        return generateResult(code, msg, (Object)null);
    }

    public static Response generateResult(int code, String msg, Object data) {
        return new Response(code, msg, data);
    }

    public static Response generateResult(CodeEnum codeEnum, Object data) {
        return generateResult(codeEnum.getCode(), codeEnum.getMsg(), data);
    }
}
