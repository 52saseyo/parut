package com.parut.order.payment.application;

import com.parut.order.delivery.application.port.in.DeliveryCreateUseCase;
import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.in.*;
import com.parut.order.order.application.port.in.dto.OrderItemSnapshotView;
import com.parut.order.order.application.port.in.dto.OrderSnapshotView;
import com.parut.order.order.domain.OrderStatus;
import com.parut.order.payment.application.dto.PaymentConfirmCommand;
import com.parut.order.payment.application.dto.PaymentConfirmContext;
import com.parut.order.payment.application.dto.PaymentConfirmResult;
import com.parut.order.payment.application.dto.PaymentReadyCommand;
import com.parut.order.payment.application.dto.PaymentReadyResult;
import com.parut.order.payment.application.port.out.PaymentGateway;
import com.parut.order.payment.application.port.out.dto.PaymentApproveResult;
import com.parut.order.payment.domain.Payment;
import com.parut.order.payment.domain.PaymentMethod;
import com.parut.order.payment.domain.PaymentStatus;
import com.parut.order.payment.domain.PaymentTransaction;
import com.parut.order.payment.domain.TransactionStatus;
import com.parut.order.payment.domain.TransactionType;
import com.parut.order.payment.infrastructure.persistence.PaymentRepository;
import com.parut.order.payment.infrastructure.persistence.PaymentTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final UUID USER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b1");
    private static final UUID ORDER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b2");
    private static final UUID PRODUCT_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b3");
    private static final String ORDER_NO = "ORD-20260908-AAAAAAAA";
    private static final String IDEMPOTENCY_KEY = "idem-key-0001";

    @Mock
    private OrderSnapshotQueryUseCase orderSnapshotQueryUseCase;

    @Mock
    private OrderStatusUseCase orderStatusUseCase;

    @Mock
    private OrderCancelUseCase orderCancelUseCase;

    @Mock
    private OrderDeliveryGroupQueryUseCase orderDeliveryGroupQueryUseCase;

    @Mock
    private OrderDeliveryGroupStatusUseCase orderDeliveryGroupStatusUseCase;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private DeliveryCreateUseCase deliveryCreateUseCase;

    @InjectMocks
    private PaymentService paymentService;

    private OrderSnapshotView orderSnapshot(OrderStatus status) {
        return new OrderSnapshotView(ORDER_ID, USER_ID, ORDER_NO, status, 33_000L, Instant.now().plusSeconds(3600));
    }

    private OrderItemSnapshotView itemSnapshot() {
        return new OrderItemSnapshotView(UUID.randomUUID(), PRODUCT_ID, "신고배 5kg 특품");
    }

    private <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }

    @Test
    @DisplayName("결제 준비: STOCK_RESERVED 주문이면 결제를 생성하고 PAYMENT_PENDING으로 전이한다")
    void 결제준비_성공() {
        OrderSnapshotView order = orderSnapshot(OrderStatus.STOCK_RESERVED);
        when(orderSnapshotQueryUseCase.getOrderSnapshot(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(orderSnapshotQueryUseCase.getFirstOrderItemSnapshot(ORDER_ID)).thenReturn(Optional.of(itemSnapshot()));

        PaymentReadyCommand command = new PaymentReadyCommand(ORDER_ID, USER_ID, PaymentMethod.CREDIT_CARD);

        PaymentReadyResult result = paymentService.ready(command);

        assertThat(result.tossOrderId()).isEqualTo(ORDER_NO);
        assertThat(result.amount()).isEqualTo(order.totalPaymentAmount());
        verify(orderStatusUseCase).markPaymentPending(ORDER_ID, USER_ID);
        verify(paymentGateway).ready(ORDER_NO, order.totalPaymentAmount());
    }

    @Test
    @DisplayName("결제 준비: STOCK_RESERVED가 아닌 주문이면 INVALID_ORDER_STATUS를 던진다")
    void 결제준비_주문상태오류() {
        when(orderSnapshotQueryUseCase.getOrderSnapshot(ORDER_ID)).thenReturn(Optional.of(orderSnapshot(OrderStatus.CREATED)));

        PaymentReadyCommand command = new PaymentReadyCommand(ORDER_ID, USER_ID, PaymentMethod.CREDIT_CARD);

        assertThatThrownBy(() -> paymentService.ready(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_STATUS);

        verifyNoInteractions(paymentRepository, paymentGateway);
    }

    @Test
    @DisplayName("결제 승인: 요청 금액이 결제 금액과 다르면 PAYMENT_AMOUNT_MISMATCH를 던진다")
    void 결제승인_금액불일치() {
        Payment payment = withId(Payment.create(ORDER_ID, ORDER_NO, USER_ID, 33_000L, IDEMPOTENCY_KEY));
        payment.start();
        when(paymentRepository.findByOrderNo(ORDER_NO)).thenReturn(Optional.of(payment));

        PaymentConfirmCommand command = new PaymentConfirmCommand("payment-key-1", ORDER_NO, 32_999L, "idem-confirm-0001");

        assertThatThrownBy(() -> paymentService.loadForConfirm(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }

    @Test
    @DisplayName("결제 승인 반영: 주문을 PAID로 전이하고 결제를 승인 처리하며 승인 거래를 기록한다")
    void 결제승인반영_성공() {
        Payment payment = withId(Payment.create(ORDER_ID, ORDER_NO, USER_ID, 33_000L, IDEMPOTENCY_KEY));
        payment.start();
        UUID orderItemId = UUID.randomUUID();
        UUID productId = PRODUCT_ID;
        PaymentConfirmContext context = new PaymentConfirmContext(payment.getId(), ORDER_ID, USER_ID, orderItemId, productId);
        PaymentConfirmCommand command = new PaymentConfirmCommand("payment-key-1", ORDER_NO, 33_000L, "idem-confirm-0001");
        PaymentApproveResult approveResult = new PaymentApproveResult(
                PaymentMethod.CREDIT_CARD, Instant.now(), "https://mock-pg.parut.local/receipts/1", "pg-tx-approve-1"
        );

        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);

        PaymentConfirmResult result = paymentService.applyApproved(context, command, approveResult);

        verify(orderStatusUseCase).markPaid(ORDER_ID, USER_ID, approveResult.approvedAt());
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.DONE);
        assertThat(payment.getPaymentKey()).isEqualTo(command.paymentKey());
        assertThat(payment.getReceiptUrl()).isEqualTo(approveResult.receiptUrl());
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.paymentId()).isEqualTo(payment.getId());

        verify(paymentTransactionRepository).save(transactionCaptor.capture());
        PaymentTransaction transaction = transactionCaptor.getValue();
        assertThat(transaction.getPaymentId()).isEqualTo(payment.getId());
        assertThat(transaction.getTransactionType()).isEqualTo(TransactionType.APPROVE);
        assertThat(transaction.getAmount()).isEqualTo(command.amount());
        assertThat(transaction.getTransactionStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }
}
