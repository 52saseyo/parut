package com.parut.order.global.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import com.parut.order.global.auth.RequireRole;
import com.parut.order.global.auth.UserContext;
import com.parut.order.global.auth.UserRole;
import com.parut.order.global.constant.HeaderConstants;
import com.parut.order.global.exception.BusinessException;

class UserContextInterceptorTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final UserContextInterceptor interceptor = new UserContextInterceptor();

    @Test
    @DisplayName("인증 헤더가 정상이면 UserContext 를 요청 속성에 담는다")
    void 인증_성공() {
        MockHttpServletRequest request = request(USER_ID.toString(), "SELLER");

        boolean proceed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(proceed).isTrue();
        assertThat(request.getAttribute(UserContextInterceptor.USER_CONTEXT_ATTRIBUTE))
                .isEqualTo(UserContext.of(USER_ID, UserRole.SELLER));
    }

    @Test
    @DisplayName("X-User-Id 헤더가 없으면 401")
    void 사용자_헤더_누락() {
        assertUnauthorized(request(null, "CUSTOMER"));
    }

    @Test
    @DisplayName("X-User-Id 가 UUID 형식이 아니면 401")
    void 사용자_헤더_형식_불량() {
        assertUnauthorized(request("not-a-uuid", "CUSTOMER"));
    }

    @Test
    @DisplayName("X-User-Role 헤더가 없으면 401")
    void 역할_헤더_누락() {
        assertUnauthorized(request(USER_ID.toString(), null));
    }

    @Test
    @DisplayName("UserRole 에 없는 역할 값이면 403")
    void 알_수_없는_역할() {
        MockHttpServletRequest request = request(USER_ID.toString(), "MANAGER");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("@RequireRole 에 없는 역할이면 403")
    void 인가되지_않은_역할() {
        MockHttpServletRequest request = request(USER_ID.toString(), "SELLER");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), customerOnlyHandler()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("@RequireRole 에 포함된 역할이면 통과한다")
    void 인가된_역할() {
        MockHttpServletRequest request = request(USER_ID.toString(), "CUSTOMER");

        boolean proceed = interceptor.preHandle(request, new MockHttpServletResponse(), customerOnlyHandler());

        assertThat(proceed).isTrue();
    }

    private HandlerMethod customerOnlyHandler() {
        try {
            return new HandlerMethod(new CustomerOnlyController(), CustomerOnlyController.class.getMethod("handle"));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    static class CustomerOnlyController {

        @RequireRole(UserRole.CUSTOMER)
        public void handle() {
        }
    }

    private void assertUnauthorized(MockHttpServletRequest request) {
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode().getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private MockHttpServletRequest request(
            String userId,
            String userRole
    ) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/orders");

        if (userId != null) {
            request.addHeader(HeaderConstants.USER_ID, userId);
        }
        if (userRole != null) {
            request.addHeader(HeaderConstants.USER_ROLE, userRole);
        }

        return request;
    }
}
