package com.parut.order.refund.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parut.order.delivery.application.port.in.DeliveryCompletionQueryUseCase;
import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.in.OrderItemQueryUseCase;
import com.parut.order.order.application.port.in.OrderItemRefundUseCase;
import com.parut.order.order.application.port.in.dto.OrderItemDetailView;
import com.parut.order.order.domain.DeliveryGroupStatus;
import com.parut.order.order.domain.OrderItemStatus;
import com.parut.order.payment.application.port.in.dto.PaymentCancelView;
import com.parut.order.refund.application.dto.RefundApprovalContext;
import com.parut.order.refund.domain.Refund;
import com.parut.order.refund.domain.RefundStatus;
import com.parut.order.refund.infrastructure.persistence.RefundRepository;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    private static final UUID ORDER_ITEM_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b8");
    private static final UUID SECOND_ORDER_ITEM_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6bd");
    private static final UUID CUSTOMER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b9");
    private static final UUID ORDER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6ba");
    private static final UUID SELLER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6bb");
    private static final UUID DELIVERY_GROUP_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6bc");

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private DeliveryCompletionQueryUseCase deliveryCompletionQueryUseCase;

    @Mock
    private OrderItemQueryUseCase orderItemQueryUseCase;

    @Mock
    private OrderItemRefundUseCase orderItemRefundUseCase;

    @InjectMocks
    private RefundService refundService;

    @Test
    @DisplayName("배송완료 후 구매확정 전 주문상품의 환불을 요청한다")
    void 환불_요청_성공() {
        when(orderItemQueryUseCase.getOrderItems(List.of(ORDER_ITEM_ID))).thenReturn(List.of(orderItem()));
        when(deliveryCompletionQueryUseCase.getDeliveredAt(DELIVERY_GROUP_ID))
                .thenReturn(Optional.of(Instant.now().minus(Duration.ofDays(1))));
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Refund refund = refundService.requestRefund(ORDER_ITEM_ID, CUSTOMER_ID, "상품 불량");

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.REQUESTED);
        assertThat(refund.getRefundAmount()).isEqualTo(10_000L);
        verify(orderItemRefundUseCase).markRefundRequested(ORDER_ITEM_ID);
        verify(refundRepository).save(refund);
    }

    @Test
    @DisplayName("배송완료 전이거나 구매확정된 상품은 환불을 요청할 수 없다")
    void 환불_요청_조건_검증() {
        when(orderItemQueryUseCase.getOrderItems(List.of(ORDER_ITEM_ID)))
                .thenReturn(List.of(orderItem(OrderItemStatus.ORDERED, DeliveryGroupStatus.SHIPPED)));

        assertThatThrownBy(() -> refundService.requestRefund(ORDER_ITEM_ID, CUSTOMER_ID, "상품 불량"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REFUND_NOT_ALLOWED));
        verifyNoInteractions(orderItemRefundUseCase);
    }

    @Test
    @DisplayName("배송 완료 후 7일이 지나면 환불을 요청할 수 없다")
    void 환불_요청_기한_검증() {
        when(orderItemQueryUseCase.getOrderItems(List.of(ORDER_ITEM_ID))).thenReturn(List.of(orderItem()));
        when(deliveryCompletionQueryUseCase.getDeliveredAt(DELIVERY_GROUP_ID))
                .thenReturn(Optional.of(Instant.now().minus(Duration.ofDays(8))));

        assertThatThrownBy(() -> refundService.requestRefund(ORDER_ITEM_ID, CUSTOMER_ID, "상품 불량"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REFUND_NOT_ALLOWED));
        verifyNoInteractions(orderItemRefundUseCase);
    }

    @Test
    @DisplayName("취소되지 않은 환불 요청이 있으면 중복 요청을 거부한다")
    void 환불_중복_요청_거부() {
        when(orderItemQueryUseCase.getOrderItems(List.of(ORDER_ITEM_ID))).thenReturn(List.of(orderItem()));
        when(deliveryCompletionQueryUseCase.getDeliveredAt(DELIVERY_GROUP_ID))
                .thenReturn(Optional.of(Instant.now().minus(Duration.ofDays(1))));
        when(refundRepository.existsByOrderItemIdAndStatusNot(ORDER_ITEM_ID, RefundStatus.CANCELED))
                .thenReturn(true);

        assertThatThrownBy(() -> refundService.requestRefund(ORDER_ITEM_ID, CUSTOMER_ID, "상품 불량"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REFUND_ALREADY_REQUESTED));
        verifyNoInteractions(orderItemRefundUseCase);
    }

    @Test
    @DisplayName("본인의 요청 상태 환불을 취소한다")
    void 환불_요청_취소() {
        UUID refundId = UUID.randomUUID();
        Refund refund = Refund.request(ORDER_ITEM_ID, 10_000L, "상품 불량", Instant.now());
        when(refundRepository.findById(refundId)).thenReturn(Optional.of(refund));
        when(orderItemQueryUseCase.getOrderItems(List.of(ORDER_ITEM_ID))).thenReturn(List.of(orderItem()));

        Refund canceled = refundService.cancelRefund(refundId, CUSTOMER_ID);

        assertThat(canceled.getStatus()).isEqualTo(RefundStatus.CANCELED);
        assertThat(canceled.getCanceledAt()).isNotNull();
        verify(orderItemRefundUseCase).cancelRefundRequest(ORDER_ITEM_ID);
    }

    @Test
    @DisplayName("환불 거절")
    void 환불_거절() {
        UUID refundId = UUID.randomUUID();
        Refund refund = Refund.request(ORDER_ITEM_ID, 10_000L, "상품 불량", Instant.now());

        when(refundRepository.findById(refundId)).thenReturn(Optional.of(refund));
        when(orderItemQueryUseCase.getOrderItems(List.of(ORDER_ITEM_ID)))
                .thenReturn(List.of(orderItem(OrderItemStatus.REFUND_REQUESTED, DeliveryGroupStatus.DELIVERED)));

        Refund rejected = refundService.rejectRefund(refundId, SELLER_ID, "환불 거절 사유");

        assertThat(rejected.getStatus()).isEqualTo(RefundStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("환불 거절 사유");
        assertThat(rejected.getProcessedAt()).isNotNull();
        assertThat(rejected.getProcessedBy()).isEqualTo(SELLER_ID);
        verify(orderItemRefundUseCase).confirmRejectedRefund(ORDER_ITEM_ID);
    }

    @Test
    @DisplayName("다른 판매자의 환불 거절 요청은 거부한다")
    void 환불_거절_권한_검증() {
        UUID refundId = UUID.randomUUID();
        Refund refund = Refund.request(ORDER_ITEM_ID, 10_000L, "상품 불량", Instant.now());

        when(refundRepository.findById(refundId)).thenReturn(Optional.of(refund));
        when(orderItemQueryUseCase.getOrderItems(List.of(ORDER_ITEM_ID)))
                .thenReturn(List.of(orderItem(OrderItemStatus.REFUND_REQUESTED, DeliveryGroupStatus.DELIVERED)));

        assertThatThrownBy(() -> refundService.rejectRefund(refundId, UUID.randomUUID(), "환불 거절 사유"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.REQUESTED);
        verifyNoInteractions(orderItemRefundUseCase);
    }

    @Test
    @DisplayName("같은 주문과 판매자의 환불 요청을 함께 승인한다")
    void 환불_다건_승인() {
        UUID firstRefundId = UUID.randomUUID();
        UUID secondRefundId = UUID.randomUUID();
        List<UUID> refundIds = List.of(firstRefundId, secondRefundId);
        Instant requestedAt = Instant.now();
        Instant canceledAt = requestedAt.plusSeconds(1);

        Refund firstRefund = Refund.request(ORDER_ITEM_ID, 10_000L, "상품 불량", requestedAt);
        Refund secondRefund = Refund.request(SECOND_ORDER_ITEM_ID, 10_000L, "상품 파손", requestedAt);

        when(refundRepository.findAllById(refundIds)).thenReturn(List.of(firstRefund, secondRefund));
        when(orderItemQueryUseCase.getOrderItems(List.of(ORDER_ITEM_ID, SECOND_ORDER_ITEM_ID)))
                .thenReturn(List.of(
                        orderItem(ORDER_ITEM_ID, OrderItemStatus.REFUND_REQUESTED),
                        orderItem(SECOND_ORDER_ITEM_ID, OrderItemStatus.REFUND_REQUESTED)
                ));

        RefundApprovalContext context = refundService.prepareApproval(refundIds, SELLER_ID);
        List<Refund> approved = refundService.completeApproval(
                context,
                new PaymentCancelView(20_000L, canceledAt)
        );

        assertThat(context.totalRefundAmount()).isEqualTo(20_000L);
        assertThat(context.orderItemIds()).containsExactlyInAnyOrder(ORDER_ITEM_ID, SECOND_ORDER_ITEM_ID);
        assertThat(approved)
                .extracting(Refund::getStatus)
                .containsOnly(RefundStatus.APPROVED);
        assertThat(approved)
                .extracting(Refund::getProcessedBy)
                .containsOnly(SELLER_ID);
        verify(orderItemRefundUseCase).markRefunded(context.orderItemIds());
    }

    private OrderItemDetailView orderItem() {
        return orderItem(OrderItemStatus.ORDERED, DeliveryGroupStatus.DELIVERED);
    }

    private OrderItemDetailView orderItem(UUID orderItemId, OrderItemStatus itemStatus) {
        return new OrderItemDetailView(
                orderItemId,
                ORDER_ID,
                CUSTOMER_ID,
                SELLER_ID,
                DELIVERY_GROUP_ID,
                itemStatus,
                DeliveryGroupStatus.DELIVERED,
                5_000L,
                2,
                null
        );
    }

    private OrderItemDetailView orderItem(OrderItemStatus itemStatus, DeliveryGroupStatus groupStatus) {
        return new OrderItemDetailView(
                ORDER_ITEM_ID,
                ORDER_ID,
                CUSTOMER_ID,
                SELLER_ID,
                DELIVERY_GROUP_ID,
                itemStatus,
                groupStatus,
                5_000L,
                2,
                null
        );
    }
}
