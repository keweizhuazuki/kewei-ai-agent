package com.kiwi.keweiaiagent.exception;

import com.kiwi.keweiaiagent.common.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.kiwi.keweiaiagent.controller")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Void> handleBusinessException(BusinessException e) {
        return BaseResponse.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "请求参数校验失败";
        return BaseResponse.fail(40001, message);
    }

    @ExceptionHandler(BindException.class)
    public BaseResponse<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "请求参数绑定失败";
        return BaseResponse.fail(40002, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return BaseResponse.fail(40003, "请求体格式错误");
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<Void> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return BaseResponse.fail(50000, "系统异常");
    }
}
