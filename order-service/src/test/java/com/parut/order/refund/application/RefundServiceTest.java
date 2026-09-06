package com.parut.order.refund.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.refund.application.dto.RefundOrderItemSnapshot;
import com.parut.order.refund.application.port.RefundOrderItemQueryPort;
import com.parut.order.refund.domain.Refund;
import com.parut.order.refund.domain.RefundStatus;
import com.parut.order.refund.infrastructure.persistence.RefundRepository;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    private static final UUID ORDER_ITEM_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b8");
    private static final UUID CUSTOMER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b9");

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private ObjectProvider<RefundOrderItemQueryPort> orderItemQueryPortProvider;

    @Mock
    private RefundOrderItemQueryPort orderItemQueryPort;

    private RefundService refundService;

    @BeforeEach
    void setUp() {
        refundService = new RefundService(refundRepository, orderItemQueryPortProvider);
        when(orderItemQueryPortProvider.getIfAvailable()).thenReturn(orderItemQueryPort);
    }

    @Test
    @DisplayName("배송완료 후 구매확정 전 주문상품의 환불을 요청한다")
    void 환불_요청_성공() {
        when(orderItemQueryPort.findById(ORDER_ITEM_ID)).thenReturn(Optional.of(orderItem()));
        when(refundRepository.existsByOrderItemIdAndStatusNot(ORDER_ITEM_ID, RefundStatus.CANCELED))
                .thenReturn(false);
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Refund refund = refundService.requestRefund(ORDER_ITEM_ID, CUSTOMER_ID, "상품 불량");

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.REQUESTED);
        assertThat(refund.getRefundAmount()).isEqualTo(10_000L);
        verify(refundRepository).save(refund);
    }

    @Test
    @DisplayName("배송완료 전이거나 구매확정된 상품은 환불을 요청할 수 없다")
    void 환불_요청_조건_검증() {
        RefundOrderItemSnapshot notDelivered = new RefundOrderItemSnapshot(
                ORDER_ITEM_ID, CUSTOMER_ID, 10_000L, false, false, false
        );
        when(orderItemQueryPort.findById(ORDER_ITEM_ID)).thenReturn(Optional.of(notDelivered));

        assertThatThrownBy(() -> refundService.requestRefund(ORDER_ITEM_ID, CUSTOMER_ID, "상품 불량"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REFUND_NOT_ALLOWED));
    }

    @Test
    @DisplayName("취소되지 않은 환불 요청이 있으면 중복 요청을 거부한다")
    void 환불_중복_요청_거부() {
        when(orderItemQueryPort.findById(ORDER_ITEM_ID)).thenReturn(Optional.of(orderItem()));
        when(refundRepository.existsByOrderItemIdAndStatusNot(ORDER_ITEM_ID, RefundStatus.CANCELED))
                .thenReturn(true);

        assertThatThrownBy(() -> refundService.requestRefund(ORDER_ITEM_ID, CUSTOMER_ID, "상품 불량"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REFUND_ALREADY_REQUESTED));
    }

    @Test
    @DisplayName("본인의 요청 상태 환불을 취소한다")
    void 환불_요청_취소() {
        Refund refund = Refund.request(ORDER_ITEM_ID, 10_000L, "상품 불량", java.time.Instant.now());
        when(refundRepository.findById(any(UUID.class))).thenReturn(Optional.of(refund));
        when(orderItemQueryPort.findById(ORDER_ITEM_ID)).thenReturn(Optional.of(orderItem()));

        Refund canceled = refundService.cancelRefund(UUID.randomUUID(), CUSTOMER_ID);

        assertThat(canceled.getStatus()).isEqualTo(RefundStatus.CANCELED);
        assertThat(canceled.getCanceledAt()).isNotNull();
    }

    private RefundOrderItemSnapshot orderItem() {
        return new RefundOrderItemSnapshot(
                ORDER_ITEM_ID,
                CUSTOMER_ID,
                10_000L,
                true,
                false,
                false
        );
    }
}
