package com.parut.order.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
import com.parut.order.payment.application.port.in.dto.PaymentCancelReceipt;
import com.parut.order.payment.application.port.in.dto.PaymentCancelView;
import com.parut.order.payment.application.port.out.PaymentGateway;
import com.parut.order.payment.application.port.out.dto.PaymentCancelResult;
import com.parut.order.payment.domain.Payment;
import com.parut.order.payment.domain.PaymentMethod;
import com.parut.order.payment.domain.PaymentStatus;
import com.parut.order.payment.domain.PaymentTransaction;
import com.parut.order.payment.infrastructure.persistence.PaymentRepository;
import com.parut.order.payment.infrastructure.persistence.PaymentTransactionRepository;

@ExtendWith(MockitoExtension.class)
class PaymentCancelServiceTest {

    private static final UUID ORDER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b1");
    private static final String PAYMENT_KEY = "toss-payment-key-0001";
    private static final String PG_TRANSACTION_KEY = "toss-tx-key-0001";
    private static final Instant CANCELED_AT = Instant.parse("2026-09-16T01:00:00Z");
    private static final long TOTAL_AMOUNT = 33_000L;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private PaymentCancelService paymentCancelService;

    @Test
    @DisplayName("PG 취소: 결제 완료 건이면 PG를 호출하고 증표를 돌려준다")
    void PG취소_성공() {
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(paidPayment()));
        when(paymentGateway.cancel(PAYMENT_KEY, 15_000L, "CUSTOMER_CANCEL"))
                .thenReturn(new PaymentCancelResult(CANCELED_AT, PG_TRANSACTION_KEY));

        Optional<PaymentCancelReceipt> receipt = paymentCancelService.cancelOnPg(ORDER_ID, 15_000L, "CUSTOMER_CANCEL");

        assertThat(receipt).isPresent();
        assertThat(receipt.get().cancelAmount()).isEqualTo(15_000L);
        assertThat(receipt.get().pgTransactionKey()).isEqualTo(PG_TRANSACTION_KEY);
        assertThat(receipt.get().canceledAt()).isEqualTo(CANCELED_AT);
    }

    @Test
    @DisplayName("PG 취소: 결제되지 않은 주문이면 PG를 호출하지 않고 빈 값을 돌려준다")
    void PG취소_미결제() {
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(readyPayment()));

        Optional<PaymentCancelReceipt> receipt = paymentCancelService.cancelOnPg(ORDER_ID, 15_000L, "CUSTOMER_CANCEL");

        assertThat(receipt).isEmpty();
        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("PG 취소: 취소 금액이 잔액을 넘으면 PG를 호출하지 않고 예외를 던진다")
    void PG취소_잔액초과() {
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(paidPayment()));

        assertThatThrownBy(() -> paymentCancelService.cancelOnPg(ORDER_ID, TOTAL_AMOUNT + 1, "CUSTOMER_CANCEL"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);

        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("PG 취소: PG 호출이 실패하면 PG_CANCEL_FAILED로 변환한다")
    void PG취소_실패() {
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(paidPayment()));
        when(paymentGateway.cancel(anyString(), anyLong(), anyString())).thenThrow(new IllegalStateException("PG 장애"));

        assertThatThrownBy(() -> paymentCancelService.cancelOnPg(ORDER_ID, 15_000L, "CUSTOMER_CANCEL"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PG_CANCEL_FAILED);

        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("원장 반영: 일부만 취소하면 PARTIAL_CANCELED가 되고 취소 거래가 기록된다")
    void 원장반영_부분취소() {
        Payment payment = paidPayment();
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));

        PaymentCancelView view = paymentCancelService.applyCancellation(ORDER_ID, receipt(15_000L));

        assertThat(view.paymentStatus()).isEqualTo(PaymentStatus.PARTIAL_CANCELED);
        assertThat(view.balanceAmount()).isEqualTo(TOTAL_AMOUNT - 15_000L);
        assertThat(view.canceledAmount()).isEqualTo(15_000L);
        assertThat(view.paymentTransactionId()).isNotNull();
    }

    @Test
    @DisplayName("원장 반영: 잔액을 전부 취소하면 CANCELED가 된다")
    void 원장반영_전액취소() {
        Payment payment = paidPayment();
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));

        PaymentCancelView view = paymentCancelService.applyCancellation(ORDER_ID, receipt(TOTAL_AMOUNT));

        assertThat(view.paymentStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(view.balanceAmount()).isZero();
    }

    private PaymentCancelReceipt receipt(long cancelAmount) {
        return new PaymentCancelReceipt(cancelAmount, "CUSTOMER_CANCEL", CANCELED_AT, PG_TRANSACTION_KEY);
    }

    private Payment readyPayment() {
        return withId(Payment.create(ORDER_ID, "ORD-20260916-AAAAAAAA", UUID.randomUUID(), TOTAL_AMOUNT, "idem-key-0001"));
    }

    private Payment paidPayment() {
        Payment payment = readyPayment();
        payment.start();
        payment.approve(PAYMENT_KEY, PaymentMethod.CREDIT_CARD, Instant.parse("2026-09-16T00:00:00Z"), null);
        return payment;
    }

    private <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }
}
