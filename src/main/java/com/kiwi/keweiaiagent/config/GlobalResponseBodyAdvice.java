package com.kiwi.keweiaiagent.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiwi.keweiaiagent.common.BaseResponse;
import org.reactivestreams.Publisher;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestControllerAdvice(basePackages = "com.kiwi.keweiaiagent.controller")
public class GlobalResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (body instanceof SseEmitter
                || body instanceof Publisher<?>
                || MediaType.TEXT_EVENT_STREAM.includes(selectedContentType)) {
            return body;
        }
        if (body instanceof BaseResponse<?>) {
            return body;
        }
        if (selectedConverterType == StringHttpMessageConverter.class) {
            try {
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return OBJECT_MAPPER.writeValueAsString(BaseResponse.success(body));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("响应序列化失败", e);
            }
        }
        return BaseResponse.success(body);
    }
}
