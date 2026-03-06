package com.kiwi.keweiaiagent.exception;

public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        this(ErrorCode.SYSTEM_ERROR.getCode(), message);
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
    }

    public Integer getCode() {
        return code;
    }
}
