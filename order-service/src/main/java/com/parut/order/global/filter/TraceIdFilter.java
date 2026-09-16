package com.parut.order.global.filter;

import com.parut.order.global.constant.HeaderConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * X-Trace-Id 를 MDC 에 올려 로그, 응답, Feign 전파가 같은 값을 사용
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String MDC_KEY = "traceId";
    private static final String INTERNAL_PATH_PREFIX = "/api/v1/internal/";

    public static String currentTraceId() {
        return MDC.get(MDC_KEY);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = resolveTraceId(request);

        MDC.put(MDC_KEY, traceId);
        response.setHeader(HeaderConstants.TRACE_ID, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(HeaderConstants.TRACE_ID);

        if (StringUtils.hasText(traceId)) {
            return traceId;
        }

        // 클라이언트 인입은 Gateway 가 항상 발급하므로, 없다면 Gateway 를 거치지 않은 호출이다.
        // 서비스 간 내부 API 호출에서 없다는 건 호출자의 전파 누락이라 추적 체인이 끊긴다.
        if (request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX)) {
            log.warn("[Trace] 내부 호출에 X-Trace-Id 가 없습니다 — 호출자의 전파 누락. uri={}", request.getRequestURI());
        }

        return UUID.randomUUID().toString();
    }
}
