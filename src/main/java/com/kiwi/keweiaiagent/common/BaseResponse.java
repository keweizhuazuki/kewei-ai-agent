package com.kiwi.keweiaiagent.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kiwi.keweiaiagent.exception.ErrorCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {

    private Integer code;
    private String message;
    private T data;

    public BaseResponse() {
    }

    public BaseResponse(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, "success", data);
    }

    public static BaseResponse<Void> success() {
        return new BaseResponse<>(0, "success", null);
    }

    public static <T> BaseResponse<T> fail(Integer code, String message) {
        return new BaseResponse<>(code, message, null);
    }

    public static <T> BaseResponse<T> fail(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
