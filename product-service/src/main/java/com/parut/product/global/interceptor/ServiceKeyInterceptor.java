package com.parut.product.global.interceptor;

import com.parut.product.global.constant.HeaderConstants;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Slf4j
@Component
public class ServiceKeyInterceptor implements HandlerInterceptor {

    private final String internalServiceKey;

    public ServiceKeyInterceptor(
            @Value("${internal.service-key}") String internalServiceKey
    ) {
        this.internalServiceKey = internalServiceKey;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        String requestKey = request.getHeader(HeaderConstants.SERVICE_KEY);

        if (requestKey == null || !requestKey.equals(internalServiceKey)) {
            // 키 값 자체는 로그에 남기지 않는다 — 유출되면 인증이 무력화된다.
            log.warn("[Internal-Auth] 내부 서비스 키 검증 실패 uri={}", request.getRequestURI());
            throw new BusinessException(ErrorCode.INTERNAL_AUTH_FAILED);
        }

        return true;
    }
}