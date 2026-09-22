package com.parut.order.delivery.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.parut.order.delivery.presentation.dto.request.StartDeliveryRequest;
import com.parut.order.delivery.domain.DeliveryStatus;
import com.parut.order.global.auth.RequireRole;
import com.parut.order.global.auth.UserContext;
import com.parut.order.global.auth.UserRole;

class DeliveryControllerAuthorizationTest {

    @Test
    @DisplayName("배송 목록은 고객과 판매자 역할을 허용한다")
    void 배송_목록_역할() throws NoSuchMethodException {
        assertThat(requireRole(
                "getDeliveries",
                UserContext.class,
                UUID.class,
                DeliveryStatus.class,
                String.class,
                UUID.class,
                int.class
        ).value()).containsExactly(UserRole.CUSTOMER, UserRole.SELLER);
    }

    @Test
    @DisplayName("관리자 배송 목록은 관리자 역할을 요구한다")
    void 관리자_배송_목록_역할() throws NoSuchMethodException {
        assertThat(requireRole(
                "getAdminDeliveries",
                UUID.class,
                UUID.class,
                UUID.class,
                DeliveryStatus.class,
                Pageable.class
        ).value()).containsExactly(UserRole.ADMIN);
    }

    @Test
    @DisplayName("배송 단건 조회는 고객, 판매자, 관리자 역할을 허용한다")
    void 배송_단건_조회_역할() throws NoSuchMethodException {
        assertThat(requireRole("getDelivery", UUID.class, UserContext.class).value())
                .containsExactly(UserRole.CUSTOMER, UserRole.SELLER, UserRole.ADMIN);
    }

    @Test
    @DisplayName("배송 시작은 판매자 역할을 요구한다")
    void 배송_시작_역할() throws NoSuchMethodException {
        assertThat(requireRole("startDelivery", UUID.class, UserContext.class, StartDeliveryRequest.class).value())
                .containsExactly(UserRole.SELLER);
    }

    private RequireRole requireRole(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = DeliveryController.class.getMethod(methodName, parameterTypes);
        return method.getAnnotation(RequireRole.class);
    }
}
