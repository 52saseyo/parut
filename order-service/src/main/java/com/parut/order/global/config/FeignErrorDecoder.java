package com.parut.order.global.config;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.global.exception.ErrorResponse;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            String body = Util.toString(response.body().asReader(StandardCharsets.UTF_8));
            ErrorResponse error = objectMapper.readValue(body, ErrorResponse.class);
            return new BusinessException(error.code(), error.message(), HttpStatus.valueOf(response.status()));
        } catch (Exception e) {
            log.warn("[FeignErrorDecoder] 에러 응답 파싱 실패 methodKey={}, status={}", methodKey, response.status(), e);
            return new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
