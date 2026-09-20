package com.parut.order.refund.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import com.parut.order.global.auth.RequireRole;
import com.parut.order.global.auth.UserContext;
import com.parut.order.global.auth.UserRole;
import com.parut.order.refund.domain.RefundStatus;
import com.parut.order.refund.presentation.dto.request.ApproveRefundRequest;
import com.parut.order.refund.presentation.dto.request.RejectRefundRequest;
import com.parut.order.refund.presentation.dto.request.RequestRefundRequest;

class RefundControllerAuthorizationTest {

    @Test
    @DisplayName("환불 요청과 요청 취소는 고객 역할을 요구한다")
    void 환불_요청과_취소_역할() throws NoSuchMethodException {
        assertThat(requireRole(
                "requestRefund", UUID.class, UserContext.class, RequestRefundRequest.class
        ).value()).containsExactly(UserRole.CUSTOMER);
        assertThat(requireRole(
                "cancelRefund", UUID.class, UserContext.class
        ).value()).containsExactly(UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("환불 승인과 거절은 판매자 역할을 요구한다")
    void 환불_승인과_거절_역할() throws NoSuchMethodException {
        assertThat(requireRole(
                "approveRefunds", UserContext.class, ApproveRefundRequest.class
        ).value()).containsExactly(UserRole.SELLER);
        assertThat(requireRole(
                "rejectRefund", UUID.class, UserContext.class, RejectRefundRequest.class
        ).value()).containsExactly(UserRole.SELLER);
    }

    @Test
    @DisplayName("환불 단건과 목록 조회는 고객과 판매자 역할을 허용한다")
    void 환불_조회_역할() throws NoSuchMethodException {
        assertThat(requireRole(
                "getRefund", UUID.class, UserContext.class
        ).value()).containsExactly(UserRole.CUSTOMER, UserRole.SELLER);
        assertThat(requireRole(
                "getRefunds",
                UserContext.class,
                RefundStatus.class,
                String.class,
                UUID.class,
                int.class
        ).value()).containsExactly(UserRole.CUSTOMER, UserRole.SELLER);
        assertThat(requireRole(
                "getAdminRefunds", RefundStatus.class, Pageable.class
        ).value()).containsExactly(UserRole.ADMIN);
    }

    private RequireRole requireRole(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = RefundController.class.getMethod(methodName, parameterTypes);
        return method.getAnnotation(RequireRole.class);
    }
}
