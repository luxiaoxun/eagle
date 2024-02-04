package com.alarm.eagle.response;

public enum ResponseCode {
    SUCCESS("200", "success"),
    SYSTEM_ERROR("500", "server_error"),

    RULE_NOT_FOUND("10010001", "rule_not_found");

    private String code;
    private String msg;

    private ResponseCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}

