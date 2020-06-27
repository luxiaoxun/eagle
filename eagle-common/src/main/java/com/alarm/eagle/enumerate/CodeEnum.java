package com.alarm.eagle.enumerate;

public enum CodeEnum {
    SUCCESS(0, "success"),
    SERVER_ERROR(-1, "server_error");

    private int code;
    private String msg;

    private CodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}

