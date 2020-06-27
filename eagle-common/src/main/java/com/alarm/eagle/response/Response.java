package com.alarm.eagle.response;

public class Response<T> {
    private int errorCode;
    private String errorMsg;
    private T data;

    public Response() {
    }

    public Response(int errorCode, String errorMsg, T data) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
        this.data = data;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMsg() {
        return this.errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String toString() {
        return "Response{errorCode=" + this.errorCode + ", errorMsg=\'" + this.errorMsg + '\'' + ", data=" + this.data + '}';
    }
}

