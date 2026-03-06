package com.kiwi.keweiaiagent.exception;

import com.kiwi.keweiaiagent.common.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
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
                : ErrorCode.INVALID_PARAM.getMessage();
        return BaseResponse.fail(ErrorCode.INVALID_PARAM.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    public BaseResponse<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : ErrorCode.PARAM_BIND_ERROR.getMessage();
        return BaseResponse.fail(ErrorCode.PARAM_BIND_ERROR.getCode(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return BaseResponse.fail(ErrorCode.REQUEST_BODY_FORMAT_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<Void> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return BaseResponse.fail(ErrorCode.SYSTEM_ERROR);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsableException(AsyncRequestNotUsableException e) {
        // SSE 客户端主动断开时（浏览器关闭、前端主动 close）属于预期行为，不再包装成 JSON 响应
        log.info("SSE client disconnected: {}", e.getMessage());
    }
}
