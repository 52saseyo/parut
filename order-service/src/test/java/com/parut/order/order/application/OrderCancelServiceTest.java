package com.parut.order.order.application;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.dto.CancelOrderCommand;
import com.parut.order.order.application.dto.OrderCancelContext;
import com.parut.order.order.application.dto.OrderCancelResult;
import com.parut.order.order.domain.*;
import com.parut.order.order.infrastructure.persistence.OrderCancelRepository;
import com.parut.order.order.infrastructure.persistence.OrderDeliveryGroupRepository;
import com.parut.order.order.infrastructure.persistence.OrderItemRepository;
import com.parut.order.order.infrastructure.persistence.OrderRepository;
import com.parut.order.payment.application.port.in.PaymentCancelUseCase;
import com.parut.order.payment.application.port.in.PaymentQueryUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCancelServiceTest {

    private static final UUID ORDER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b1");
    private static final UUID CUSTOMER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b2");
    private static final UUID SELLER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b3");
    private static final long UNIT_PRICE = 15_000L;
    private static final long DELIVERY_FEE = 3_000L;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderDeliveryGroupRepository orderDeliveryGroupRepository;

    @Mock
    private OrderCancelRepository orderCancelRepository;

    @Mock
    private PaymentCancelUseCase paymentCancelUseCase;

    @Mock
    private PaymentQueryUseCase paymentQueryUseCase;

    @InjectMocks
    private OrderCancelService orderCancelService;

    @Test
    @DisplayName("재고 확정 실패 시 모든 배송그룹·아이템을 취소하고 취소 금액에 전부 반영한다")
    void 재고확정실패_다건전체취소() {
        ReflectionTestUtils.setField(orderCancelService, "systemAccountId", "00000000-0000-0000-0000-000000000000");
        // 배송그룹 2개라 배송비도 2배 필요 -> paidOrder()의 단일 배송비 전제와 달라 별도 구성
        Order order = Order.create(
                "ORD-20260916-BBBBBBBB", CUSTOMER_ID, OrderType.NORMAL, "김파릇", "010-1234-5678",
                "12345", "전남 나주시 배꽃로 1", null, null, UNIT_PRICE * 2, DELIVERY_FEE * 2, "idem-order-0002");
        ReflectionTestUtils.setField(order, "id", ORDER_ID);
        order.markStockReserved(Instant.parse("2026-09-17T01:00:00Z"));
        order.markPaymentPending();
        order.markPaid(Instant.parse("2026-09-16T01:00:00Z"));
        OrderDeliveryGroup group1 = group(DeliveryGroupStatus.PENDING, SELLER_ID);
        OrderDeliveryGroup group2 = group(DeliveryGroupStatus.PENDING, UUID.randomUUID());
        OrderItem item1 = item(group1, null);
        OrderItem item2 = item(group2, null);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderDeliveryGroupRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(group1, group2));
        when(orderItemRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(item1, item2));
        when(orderCancelRepository.save(any(OrderCancel.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));

        orderCancelService.cancelForStockShortage(ORDER_ID);

        assertThat(item1.getItemStatus()).isEqualTo(OrderItemStatus.CANCELED);
        assertThat(item2.getItemStatus()).isEqualTo(OrderItemStatus.CANCELED);
        assertThat(group1.getGroupStatus()).isEqualTo(DeliveryGroupStatus.CANCELED);
        assertThat(group2.getGroupStatus()).isEqualTo(DeliveryGroupStatus.CANCELED);
        assertThat(order.getCanceledAmount()).isEqualTo(UNIT_PRICE * 2 + DELIVERY_FEE * 2);
    }

    @Test
    @DisplayName("검증: 그룹의 ORDERED 아이템이 모두 취소 대상이면 배송비까지 취소 금액에 포함한다")
    void 금액계산_전체취소() {
        OrderDeliveryGroup group = group(DeliveryGroupStatus.PENDING, SELLER_ID);
        OrderItem first = item(group, null);
        OrderItem second = item(group, null);
        stubLookup(paidOrder(), group, List.of(first, second));

        OrderCancelContext context = orderCancelService.loadForCancel(
                command(List.of(first.getId(), second.getId()), CanceledByType.CUSTOMER, CUSTOMER_ID));

        assertThat(context.cancelProductAmount()).isEqualTo(UNIT_PRICE * 2);
        assertThat(context.cancelDeliveryFee()).isEqualTo(DELIVERY_FEE);
        assertThat(context.cancelTotalAmount()).isEqualTo(UNIT_PRICE * 2 + DELIVERY_FEE);
    }

    @Test
    @DisplayName("검증: 그룹에 ORDERED 아이템이 남으면 배송비는 취소하지 않는다")
    void 금액계산_부분취소() {
        OrderDeliveryGroup group = group(DeliveryGroupStatus.PENDING, SELLER_ID);
        OrderItem target = item(group, null);
        OrderItem remaining = item(group, null);
        stubLookup(paidOrder(), group, List.of(target, remaining));

        OrderCancelContext context = orderCancelService.loadForCancel(
                command(List.of(target.getId()), CanceledByType.CUSTOMER, CUSTOMER_ID));

        assertThat(context.cancelProductAmount()).isEqualTo(UNIT_PRICE);
        assertThat(context.cancelDeliveryFee()).isZero();
    }

    @Test
    @DisplayName("검증: 여러 배송그룹을 걸쳐 일부만 취소하면 어느 그룹의 배송비도 취소하지 않는다")
    void 금액계산_다중그룹_부분취소() {
        OrderDeliveryGroup groupA = group(DeliveryGroupStatus.PREPARING, SELLER_ID);
        OrderDeliveryGroup groupB = group(DeliveryGroupStatus.PREPARING, UUID.randomUUID());
        List<OrderItem> itemsOfA = List.of(item(groupA, null), item(groupA, null), item(groupA, null));
        List<OrderItem> itemsOfB = List.of(item(groupB, null), item(groupB, null));
        stubLookup(paidOrder(), List.of(groupA, groupB), concat(itemsOfA, itemsOfB));

        List<UUID> targetIds = List.of(
                itemsOfA.get(0).getId(), itemsOfA.get(1).getId(), itemsOfB.get(0).getId());

        OrderCancelContext context = orderCancelService.loadForCancel(
                command(targetIds, CanceledByType.CUSTOMER, CUSTOMER_ID));

        assertThat(context.cancelProductAmount()).isEqualTo(UNIT_PRICE * 3);
        assertThat(context.cancelDeliveryFee()).isZero();
        assertThat(context.items()).hasSize(3);
    }

    @Test
    @DisplayName("검증: 여러 배송그룹 중 한 그룹만 전부 취소하면 그 그룹의 배송비만 취소한다")
    void 금액계산_다중그룹_한그룹만_전체취소() {
        OrderDeliveryGroup groupA = group(DeliveryGroupStatus.PREPARING, SELLER_ID);
        OrderDeliveryGroup groupB = group(DeliveryGroupStatus.PREPARING, UUID.randomUUID());
        List<OrderItem> itemsOfA = List.of(item(groupA, null), item(groupA, null));
        List<OrderItem> itemsOfB = List.of(item(groupB, null), item(groupB, null));
        stubLookup(paidOrder(), List.of(groupA, groupB), concat(itemsOfA, itemsOfB));

        List<UUID> targetIds = List.of(
                itemsOfA.get(0).getId(), itemsOfA.get(1).getId(), itemsOfB.get(0).getId());

        OrderCancelContext context = orderCancelService.loadForCancel(
                command(targetIds, CanceledByType.CUSTOMER, CUSTOMER_ID));

        assertThat(context.cancelProductAmount()).isEqualTo(UNIT_PRICE * 3);
        assertThat(context.cancelDeliveryFee()).isEqualTo(DELIVERY_FEE);
    }

    @Test
    @DisplayName("검증: 배송이 시작된 그룹의 아이템은 ORDER_ALREADY_SHIPPED로 막는다")
    void 검증_배송시작() {
        OrderDeliveryGroup group = group(DeliveryGroupStatus.SHIPPED, SELLER_ID);
        OrderItem target = item(group, null);
        stubLookup(paidOrder(), group, List.of(target));

        CancelOrderCommand command = command(List.of(target.getId()), CanceledByType.CUSTOMER, CUSTOMER_ID);

        assertThatThrownBy(() -> orderCancelService.loadForCancel(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ALREADY_SHIPPED);
    }

    @Test
    @DisplayName("검증: 이미 취소된 아이템은 INVALID_ORDER_STATUS로 막는다")
    void 검증_이미취소됨() {
        OrderDeliveryGroup group = group(DeliveryGroupStatus.PENDING, SELLER_ID);
        OrderItem target = item(group, null);
        target.cancel(UUID.randomUUID());
        stubLookup(paidOrder(), group, List.of(target));

        CancelOrderCommand command = command(List.of(target.getId()), CanceledByType.CUSTOMER, CUSTOMER_ID);

        assertThatThrownBy(() -> orderCancelService.loadForCancel(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
    }

    @Test
    @DisplayName("검증: 판매자는 자기 배송그룹이 아닌 아이템을 취소할 수 없다")
    void 검증_타판매자() {
        OrderDeliveryGroup group = group(DeliveryGroupStatus.PENDING, SELLER_ID);
        OrderItem target = item(group, null);
        stubLookup(paidOrder(), group, List.of(target));

        CancelOrderCommand command = command(List.of(target.getId()), CanceledByType.SELLER, UUID.randomUUID());

        assertThatThrownBy(() -> orderCancelService.loadForCancel(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);
    }

    @Test
    @DisplayName("검증: 구매자는 타인의 주문을 취소할 수 없다")
    void 검증_타인주문() {
        OrderDeliveryGroup group = group(DeliveryGroupStatus.PENDING, SELLER_ID);
        OrderItem target = item(group, null);
        stubLookup(paidOrder(), group, List.of(target));

        CancelOrderCommand command = command(List.of(target.getId()), CanceledByType.CUSTOMER, UUID.randomUUID());

        assertThatThrownBy(() -> orderCancelService.loadForCancel(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);
    }

    @Test
    @DisplayName("반영: 전체 취소하면 그룹과 아이템이 취소되고 주문은 PAID를 유지한 채 취소금액만 증가한다")
    void 반영_전체취소() {
        Order order = paidOrder();
        OrderDeliveryGroup group = group(DeliveryGroupStatus.PENDING, SELLER_ID);
        OrderItem target = item(group, null);
        stubLookup(order, group, List.of(target));
        stubCancelSave();

        CancelOrderCommand command = command(List.of(target.getId()), CanceledByType.CUSTOMER, CUSTOMER_ID);
        OrderCancelResult result = orderCancelService.applyCancel(command, context(target, UNIT_PRICE, DELIVERY_FEE), null);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getCanceledAmount()).isEqualTo(UNIT_PRICE + DELIVERY_FEE);
        assertThat(group.getGroupStatus()).isEqualTo(DeliveryGroupStatus.CANCELED);
        assertThat(target.getItemStatus()).isEqualTo(OrderItemStatus.CANCELED);
        assertThat(result.refundRequired()).isFalse();
        assertThat(result.payment()).isNull();
        verifyNoInteractions(paymentCancelUseCase);
    }

    @Test
    @DisplayName("반영: 판매자가 자기 배송그룹 아이템을 취소하면 SELLER_CANCEL/SELLER로 기록된다")
    void 반영_판매자취소() {
        Order order = paidOrder();
        OrderDeliveryGroup group = group(DeliveryGroupStatus.PENDING, SELLER_ID);
        OrderItem target = item(group, null);
        stubLookup(order, group, List.of(target));
        stubCancelSave();

        CancelOrderCommand command = command(List.of(target.getId()), CanceledByType.SELLER, SELLER_ID);
        OrderCancelResult result = orderCancelService.applyCancel(command, context(target, UNIT_PRICE, DELIVERY_FEE), null);

        assertThat(result.cancelReasonCode()).isEqualTo(CancelReasonCode.SELLER_CANCEL);
        assertThat(result.canceledByType()).isEqualTo(CanceledByType.SELLER);
    }

    @Test
    @DisplayName("반영: 결제 전 주문의 아이템이 모두 취소되면 주문이 ABORTED로 전이한다")
    void 반영_결제전_전체취소() {
        Order order = stockReservedOrder();
        OrderDeliveryGroup group = group(DeliveryGroupStatus.PENDING, SELLER_ID);
        OrderItem target = item(group, null);
        stubLookup(order, group, List.of(target));
        stubCancelSave();

        CancelOrderCommand command = command(List.of(target.getId()), CanceledByType.CUSTOMER, CUSTOMER_ID);
        orderCancelService.applyCancel(command, context(target, UNIT_PRICE, DELIVERY_FEE), null);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ABORTED);
    }

    private void stubLookup(Order order, OrderDeliveryGroup group, List<OrderItem> items) {
        stubLookup(order, List.of(group), items);
    }

    private void stubLookup(Order order, List<OrderDeliveryGroup> groups, List<OrderItem> items) {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderDeliveryGroupRepository.findByOrderId(ORDER_ID)).thenReturn(groups);
        when(orderItemRepository.findByOrderId(ORDER_ID)).thenReturn(items);
    }

    private List<OrderItem> concat(List<OrderItem> first, List<OrderItem> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }

    private void stubCancelSave() {
        when(orderCancelRepository.save(any(OrderCancel.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    }

    private OrderCancelContext context(OrderItem target, long productAmount, long deliveryFee) {
        return new OrderCancelContext(
                ORDER_ID,
                productAmount,
                deliveryFee,
                productAmount + deliveryFee,
                List.of(new OrderCancelContext.CancelTargetItem(
                        target.getId(), target.getProductId(), target.getTimeDealId()))
        );
    }

    private CancelOrderCommand command(List<UUID> orderItemIds, CanceledByType canceledByType, UUID requesterId) {
        CancelReasonCode reasonCode = canceledByType == CanceledByType.SELLER
                ? CancelReasonCode.SELLER_CANCEL
                : CancelReasonCode.CUSTOMER_CANCEL;

        return new CancelOrderCommand(
                ORDER_ID, orderItemIds, reasonCode, "사유", canceledByType, requesterId, "idem-key-0001");
    }

    private Order paidOrder() {
        Order order = stockReservedOrder();
        order.markPaymentPending();
        order.markPaid(Instant.parse("2026-09-16T01:00:00Z"));
        return order;
    }

    private Order stockReservedOrder() {
        Order order = Order.create(
                "ORD-20260916-AAAAAAAA", CUSTOMER_ID, OrderType.NORMAL, "김파릇", "010-1234-5678",
                "12345", "전남 나주시 배꽃로 1", null, null, UNIT_PRICE * 2, DELIVERY_FEE, "idem-order-0001");
        ReflectionTestUtils.setField(order, "id", ORDER_ID);
        order.markStockReserved(Instant.parse("2026-09-17T01:00:00Z"));
        return order;
    }

    private OrderDeliveryGroup group(DeliveryGroupStatus status, UUID sellerId) {
        OrderDeliveryGroup group = withId(OrderDeliveryGroup.create(ORDER_ID, sellerId, UNIT_PRICE * 2, DELIVERY_FEE));
        if (status == DeliveryGroupStatus.PREPARING || status == DeliveryGroupStatus.SHIPPED) {
            group.markPreparing();
        }
        if (status == DeliveryGroupStatus.SHIPPED) {
            group.markShipped();
        }
        return group;
    }

    private OrderItem item(OrderDeliveryGroup group, UUID timeDealId) {
        return withId(OrderItem.create(
                ORDER_ID, group.getId(), timeDealId == null ? UUID.randomUUID() : null, timeDealId,
                "신고배 5kg 특품", "NORMAL", "국내산(전남 나주)", LocalDate.of(2026, 8, 20), "KG",
                BigDecimal.valueOf(5), UNIT_PRICE, UNIT_PRICE, 1));
    }

    private <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }
}
