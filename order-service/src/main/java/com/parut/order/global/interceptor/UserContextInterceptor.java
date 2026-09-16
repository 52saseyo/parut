package com.parut.order.global.interceptor;

import com.parut.order.global.auth.RequireRole;
import com.parut.order.global.auth.UserContext;
import com.parut.order.global.auth.UserRole;
import com.parut.order.global.constant.HeaderConstants;
import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.UUID;


/**
 * Gateway 가 전파한 인증 헤더를 검증해 {@link UserContext} 로 변환한다.
 */
@Slf4j
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    public static final String USER_CONTEXT_ATTRIBUTE = UserContext.class.getName();

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        UUID userId = resolveUserId(request);
        UserRole role = resolveUserRole(request);

        verifyRequiredRole(handler, role, request);

        request.setAttribute(USER_CONTEXT_ATTRIBUTE, UserContext.of(userId, role));

        return true;
    }

    // @RequireRole 이 없는 핸들러는 역할을 따지지 않는다
    private void verifyRequiredRole(
            Object handler,
            UserRole role,
            HttpServletRequest request
    ) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return;
        }

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);

        if (requireRole == null || Arrays.asList(requireRole.value()).contains(role)) {
            return;
        }

        log.warn(
                "[User-Auth] 역할 인가 실패 role={}, required={}, uri={}",
                role,
                Arrays.toString(requireRole.value()),
                request.getRequestURI()
        );

        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private UUID resolveUserId(HttpServletRequest request) {
        String userId = request.getHeader(HeaderConstants.USER_ID);

        if (!StringUtils.hasText(userId)) {
            throw unauthorized(request, HeaderConstants.USER_ID, "헤더 누락");
        }

        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw unauthorized(request, HeaderConstants.USER_ID, "UUID 형식 불량");
        }
    }

    // 헤더 누락은 401, role 값이 다르면 403
    private UserRole resolveUserRole(HttpServletRequest request) {
        String role = request.getHeader(HeaderConstants.USER_ROLE);

        if (!StringUtils.hasText(role)) {
            throw unauthorized(request, HeaderConstants.USER_ROLE, "헤더 누락");
        }

        return UserRole.parse(role)
                .orElseThrow(() -> unknownRole(request, role));
    }

    private BusinessException unknownRole(
            HttpServletRequest request,
            String role
    ) {
        log.warn(
                "[User-Auth] 알 수 없는 역할 값 role={}, uri={}",
                role,
                request.getRequestURI()
        );

        return new BusinessException(ErrorCode.FORBIDDEN);
    }

    private BusinessException unauthorized(
            HttpServletRequest request,
            String header,
            String reason
    ) {
        log.warn(
                "[User-Auth] 인증 헤더 검증 실패 header={}, reason={}, uri={}",
                header,
                reason,
                request.getRequestURI()
        );

        return new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
