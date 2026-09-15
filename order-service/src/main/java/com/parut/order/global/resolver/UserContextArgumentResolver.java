package com.parut.order.global.resolver;

import com.parut.order.global.auth.UserContext;
import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.global.interceptor.UserContextInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
@Component
public class UserContextArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return UserContext.class.equals(parameter.getParameterType());
    }

    @Override
    public UserContext resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        Object userContext = webRequest.getAttribute(
                UserContextInterceptor.USER_CONTEXT_ATTRIBUTE,
                RequestAttributes.SCOPE_REQUEST
        );

        // 인터셉터 등록이 빠진 경우 예외처리
        if (userContext == null) {
            log.error(
                    "[User-Auth] UserContext 미설정 — 인터셉터 적용 대상이 아닌 경로입니다. handler={}",
                    parameter.getMethod()
            );

            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return (UserContext) userContext;
    }
}
