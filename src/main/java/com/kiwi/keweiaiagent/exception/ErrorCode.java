package com.kiwi.keweiaiagent.exception;

public enum ErrorCode {

    SUCCESS(0, "success"),

    INVALID_PARAM(40001, "请求参数错误"),
    PARAM_BIND_ERROR(40002, "请求参数绑定失败"),
    REQUEST_BODY_FORMAT_ERROR(40003, "请求体格式错误"),
    AGENT_BUSY(40010, "Agent当前非空闲状态，无法重复执行"),

    AGENT_RUN_FAILED(50010, "Agent执行失败"),
    SYSTEM_ERROR(50000, "系统异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
