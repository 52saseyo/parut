package com.parut.order.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
import com.parut.order.order.infrastructure.persistence.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderItemConfirmationServiceTest {

    private static final UUID USER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b1");
    private static final UUID PRODUCT_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b2");
    private static final UUID SELLER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b3");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderDeliveryGroupRepository orderDeliveryGroupRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderItemConfirmationService orderItemConfirmationService;

    @Test
    @DisplayName("배송 완료된 본인 주문상품을 구매 확정한다")
    void 구매확정() {
        Order order = withId(order("ORD-20260909-AAAAAAAA"));
        OrderDeliveryGroup deliveryGroup = deliveryGroup(order);
        markDelivered(deliveryGroup);
        OrderItem orderItem = orderItem(order, deliveryGroup);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(orderItem.getId())).thenReturn(Optional.of(orderItem));
        when(orderDeliveryGroupRepository.findById(deliveryGroup.getId())).thenReturn(Optional.of(deliveryGroup));

        OrderItem result = orderItemConfirmationService.confirmOrderItem(order.getId(), orderItem.getId(), USER_ID);

        assertThat(result.getItemStatus()).isEqualTo(OrderItemStatus.CONFIRMED);
        assertThat(result.getConfirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("본인 주문상품이 아니면 구매 확정할 수 없다")
    void 구매확정_소유권_검증() {
        Order order = withId(order("ORD-20260909-BBBBBBBB"));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderItemConfirmationService.confirmOrderItem(
                order.getId(),
                UUID.randomUUID(),
                UUID.randomUUID()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);
    }

    @Test
    @DisplayName("배송 완료 전에는 구매 확정할 수 없다")
    void 구매확정_배송상태_검증() {
        Order order = withId(order("ORD-20260909-CCCCCCCC"));
        OrderDeliveryGroup deliveryGroup = deliveryGroup(order);
        OrderItem orderItem = orderItem(order, deliveryGroup);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(orderItem.getId())).thenReturn(Optional.of(orderItem));
        when(orderDeliveryGroupRepository.findById(deliveryGroup.getId())).thenReturn(Optional.of(deliveryGroup));

        assertThatThrownBy(() -> orderItemConfirmationService.confirmOrderItem(
                order.getId(), orderItem.getId(), USER_ID
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ITEM_CONFIRMATION_NOT_ALLOWED);
    }

    @Test
    @DisplayName("취소되거나 환불 처리 중인 주문상품은 구매 확정할 수 없다")
    void 구매확정_제외상태_검증() {
        Order order = withId(order("ORD-20260909-DDDDDDDD"));
        OrderDeliveryGroup deliveryGroup = deliveryGroup(order);
        markDelivered(deliveryGroup);

        OrderItem canceledItem = orderItem(order, deliveryGroup);
        canceledItem.cancel(UUID.randomUUID());
        OrderItem refundRequestedItem = orderItem(order, deliveryGroup);
        refundRequestedItem.requestRefund();
        OrderItem refundedItem = orderItem(order, deliveryGroup);
        refundedItem.requestRefund();
        refundedItem.markRefunded();

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderDeliveryGroupRepository.findById(deliveryGroup.getId())).thenReturn(Optional.of(deliveryGroup));

        for (OrderItem orderItem : List.of(canceledItem, refundRequestedItem, refundedItem)) {
            when(orderItemRepository.findById(orderItem.getId())).thenReturn(Optional.of(orderItem));

            assertThatThrownBy(() -> orderItemConfirmationService.confirmOrderItem(
                    order.getId(), orderItem.getId(), USER_ID
            ))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ORDER_ITEM_CONFIRMATION_NOT_ALLOWED);
        }
    }

    @Test
    @DisplayName("이미 구매 확정된 주문상품은 기존 결과를 반환한다")
    void 구매확정_재요청() {
        Order order = withId(order("ORD-20260909-EEEEEEEE"));
        OrderDeliveryGroup deliveryGroup = deliveryGroup(order);
        OrderItem orderItem = orderItem(order, deliveryGroup);
        Instant confirmedAt = Instant.parse("2026-09-09T01:00:00Z");
        orderItem.confirm(confirmedAt);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderItemRepository.findById(orderItem.getId())).thenReturn(Optional.of(orderItem));

        OrderItem result = orderItemConfirmationService.confirmOrderItem(order.getId(), orderItem.getId(), USER_ID);

        assertThat(result.getItemStatus()).isEqualTo(OrderItemStatus.CONFIRMED);
        assertThat(result.getConfirmedAt()).isEqualTo(confirmedAt);
    }

    private Order order(String orderNo) {
        return Order.create(
                orderNo,
                USER_ID,
                OrderType.NORMAL,
                "홍길동",
                "01012345678",
                "06234",
                "서울특별시 강남구 테헤란로 123",
                null,
                null,
                30_000L,
                3_000L,
                "confirmation-idem-key"
        );
    }

    private OrderDeliveryGroup deliveryGroup(Order order) {
        return withId(OrderDeliveryGroup.create(order.getId(), SELLER_ID, 15_000L, 3_000L));
    }

    private OrderItem orderItem(Order order, OrderDeliveryGroup deliveryGroup) {
        return withId(OrderItem.create(
                order.getId(),
                deliveryGroup.getId(),
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

    private void markDelivered(OrderDeliveryGroup deliveryGroup) {
        deliveryGroup.markPreparing();
        deliveryGroup.markShipped();
        deliveryGroup.markDelivered();
    }

    private <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }
}
