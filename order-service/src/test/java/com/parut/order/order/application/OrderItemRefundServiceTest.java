package com.parut.order.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.domain.Order;
import com.parut.order.order.domain.OrderDeliveryGroup;
import com.parut.order.order.domain.OrderItem;
import com.parut.order.order.domain.OrderItemStatus;
import com.parut.order.order.domain.OrderType;
import com.parut.order.order.infrastructure.persistence.OrderDeliveryGroupRepository;
import com.parut.order.order.infrastructure.persistence.OrderItemRepository;

@ExtendWith(MockitoExtension.class)
class OrderItemRefundServiceTest {

    private static final UUID USER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b1");
    private static final UUID PRODUCT_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b2");
    private static final UUID SELLER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b3");

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderDeliveryGroupRepository orderDeliveryGroupRepository;

    @InjectMocks
    private OrderItemRefundService orderItemRefundService;

    @Test
    @DisplayName("배송 완료된 주문상품은 환불을 요청할 수 있다")
    void 환불_요청() {
        Order order = withId(order());
        OrderDeliveryGroup group = deliveryGroup(order);
        markDelivered(group);
        OrderItem item = orderItem(order, group);

        when(orderItemRepository.findAllById(anyList())).thenReturn(List.of(item));
        when(orderDeliveryGroupRepository.findAllById(anyList())).thenReturn(List.of(group));

        orderItemRefundService.requestRefund(List.of(item.getId()));

        assertThat(item.getItemStatus()).isEqualTo(OrderItemStatus.REFUND_REQUESTED);
    }

    @Test
    @DisplayName("배송 시작 전 주문상품은 환불을 요청할 수 없다")
    void 환불_요청_상태_검증() {
        Order order = withId(order());
        OrderDeliveryGroup group = deliveryGroup(order);
        OrderItem item = orderItem(order, group);

        when(orderItemRepository.findAllById(anyList())).thenReturn(List.of(item));
        when(orderDeliveryGroupRepository.findAllById(anyList())).thenReturn(List.of(group));

        assertThatThrownBy(() -> orderItemRefundService.requestRefund(List.of(item.getId())))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
    }

    @Test
    @DisplayName("고객이 환불 요청을 철회하면 주문 상태로 돌아간다")
    void 환불_철회() {
        OrderItem item = orderItem(withId(order()), null);
        item.requestRefund();

        when(orderItemRepository.findAllById(anyList())).thenReturn(List.of(item));

        orderItemRefundService.withdrawRefundRequest(List.of(item.getId()));

        assertThat(item.getItemStatus()).isEqualTo(OrderItemStatus.ORDERED);
    }

    @Test
    @DisplayName("일부만 환불 요청 상태면 배치 전체가 실패한다")
    void 환불_완료반영_all_or_nothing() {
        OrderItem requested = orderItem(withId(order()), null);
        requested.requestRefund();
        OrderItem ordered = orderItem(withId(order()), null);

        when(orderItemRepository.findAllById(anyList())).thenReturn(List.of(requested, ordered));

        assertThatThrownBy(() -> orderItemRefundService.applyRefundCompletion(
                List.of(requested.getId(), ordered.getId())
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
        assertThat(requested.getItemStatus()).isEqualTo(OrderItemStatus.REFUND_REQUESTED);
    }

    @Test
    @DisplayName("판매자가 거절하면 정산 대상인 구매확정 상태로 전이한다")
    void 판매자_거절() {
        OrderItem item = orderItem(withId(order()), null);
        item.requestRefund();

        when(orderItemRepository.findAllById(anyList())).thenReturn(List.of(item));

        orderItemRefundService.rejectRefund(List.of(item.getId()));

        assertThat(item.getItemStatus()).isEqualTo(OrderItemStatus.CONFIRMED);
        assertThat(item.getConfirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 주문상품 ID가 섞이면 실패한다")
    void 존재하지_않는_주문상품() {
        when(orderItemRepository.findAllById(anyList())).thenReturn(List.of());

        assertThatThrownBy(() -> orderItemRefundService.withdrawRefundRequest(List.of(UUID.randomUUID())))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ITEM_NOT_FOUND);
    }

    private Order order() {
        return Order.create(
                "ORD-20260910-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                USER_ID,
                OrderType.NORMAL,
                "홍길동",
                "01012345678",
                "06234",
                "서울특별시 강남구 테헤란로 123",
                null,
                null,
                15_000L,
                3_000L,
                "refund-idem-key-" + UUID.randomUUID()
        );
    }

    private OrderDeliveryGroup deliveryGroup(Order order) {
        return withId(OrderDeliveryGroup.create(order.getId(), SELLER_ID, 15_000L, 3_000L));
    }

    private OrderItem orderItem(Order order, OrderDeliveryGroup group) {
        UUID groupId = group != null ? group.getId() : UUID.randomUUID();
        return withId(OrderItem.create(
                order.getId(),
                groupId,
                PRODUCT_ID,
                null,
                "신고배 5kg 특품",
                "NORMAL",
                "국내산(전남 나주)",
                LocalDate.of(2026, 8, 20),
                "KG",
                BigDecimal.valueOf(5),
                15_000L,
                15_000L,
                1
        ));
    }

    private void markDelivered(OrderDeliveryGroup group) {
        group.markPreparing();
        group.markShipped();
        group.markDelivered();
    }

    private <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }
}
