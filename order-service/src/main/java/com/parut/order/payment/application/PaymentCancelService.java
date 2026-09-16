package com.parut.order.payment.application;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.payment.application.port.in.PaymentCancelUseCase;
import com.parut.order.payment.application.port.in.dto.PaymentCancelReceipt;
import com.parut.order.payment.application.port.in.dto.PaymentCancelView;
import com.parut.order.payment.application.port.out.PaymentGateway;
import com.parut.order.payment.application.port.out.dto.PaymentCancelResult;
import com.parut.order.payment.domain.Payment;
import com.parut.order.payment.domain.PaymentTransaction;
import com.parut.order.payment.domain.TransactionStatus;
import com.parut.order.payment.domain.TransactionType;
import com.parut.order.payment.infrastructure.persistence.PaymentRepository;
import com.parut.order.payment.infrastructure.persistence.PaymentTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 취소, 환불이 호출하는 결제 취소 포트 구현
 *
 * <p>PG 호출과 원장 갱신을 메서드로 분리해, 호출자가 PG 호출은 트랜잭션 밖에서,
 * 원장 갱신은 자신의 트랜잭션 안에서 수행할 수 있게 합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentCancelService implements PaymentCancelUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentGateway paymentGateway;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Optional<PaymentCancelReceipt> cancelOnPg(UUID orderId, long cancelAmount, String reason) {
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment == null || !payment.isPaid()) {
            return Optional.empty();
        }
        if (cancelAmount <= 0 || cancelAmount > payment.getBalanceAmount()) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        PaymentCancelResult cancelResult;
        try {
            cancelResult = paymentGateway.cancel(payment.getPaymentKey(), cancelAmount, reason);
        } catch (RuntimeException e) {
            log.warn("[PaymentCancelService] PG 취소 실패. paymentId={}, cancelAmount={}", payment.getId(), cancelAmount, e);
            throw new BusinessException(ErrorCode.PG_CANCEL_FAILED);
        }

        return Optional.of(new PaymentCancelReceipt(
                cancelAmount, reason, cancelResult.canceledAt(), cancelResult.pgTransactionKey()
        ));
    }

    @Override
    @Transactional
    public PaymentCancelView applyCancellation(UUID orderId, PaymentCancelReceipt receipt) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        payment.applyCancellation(receipt.cancelAmount(), receipt.canceledAt());

        PaymentTransaction transaction = PaymentTransaction.create(
                payment.getId(), TransactionType.CANCEL, receipt.cancelAmount(), UUID.randomUUID().toString(), null
        );
        transaction.complete(
                TransactionStatus.SUCCESS, receipt.pgTransactionKey(), payment.getBalanceAmount(),
                receipt.reason(), null, receipt.canceledAt()
        );

        return PaymentCancelView.of(payment, paymentTransactionRepository.save(transaction).getId());
    }
}
