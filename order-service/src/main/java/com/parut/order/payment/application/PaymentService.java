package com.parut.order.payment.application;

import com.parut.order.delivery.application.port.in.DeliveryCreateUseCase;
import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.in.*;
import com.parut.order.order.application.port.in.dto.OrderDeliveryGroupView;
import com.parut.order.order.application.port.in.dto.OrderItemSnapshotView;
import com.parut.order.order.application.port.in.dto.OrderSnapshotView;
import com.parut.order.order.domain.CancelReasonCode;
import com.parut.order.order.domain.OrderStatus;
import com.parut.order.payment.application.dto.*;
import com.parut.order.payment.application.port.out.PaymentGateway;
import com.parut.order.payment.application.port.out.dto.PaymentApproveResult;
import com.parut.order.payment.application.port.out.dto.PaymentCancelResult;
import com.parut.order.payment.domain.*;
import com.parut.order.payment.infrastructure.persistence.PaymentRepository;
import com.parut.order.payment.infrastructure.persistence.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 결제 준비·승인의 DB 상태 변경을 전담합니다.
 *
 * <p>결제 승인은 PG 호출(트랜잭션 밖)과 DB 갱신(트랜잭션 안)이 여러 단계로 얽혀 있어
 * {@link PaymentFacade}가 이 클래스의 짧은 단위 메서드들을 순서대로 호출해 조율합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final OrderSnapshotQueryUseCase orderSnapshotQueryUseCase;
    private final OrderStatusUseCase orderStatusUseCase;
    private final OrderCancelUseCase orderCancelUseCase;
    private final OrderDeliveryGroupQueryUseCase orderDeliveryGroupQueryUseCase;
    private final OrderDeliveryGroupStatusUseCase orderDeliveryGroupStatusUseCase;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentGateway paymentGateway;
    private final DeliveryCreateUseCase deliveryCreateUseCase;

    @Value("${payment.success-url}")
    private String successUrl;

    @Value("${payment.fail-url}")
    private String failUrl;

    @Transactional
    public PaymentReadyResult ready(PaymentReadyCommand command) {
        OrderSnapshotView order = orderSnapshotQueryUseCase.getOrderSnapshot(command.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.userId().equals(command.userId())) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }
        if (order.orderStatus() != OrderStatus.STOCK_RESERVED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        Payment payment = paymentRepository.findByOrderId(order.orderId())
                .map(this::retryExistingPayment)
                .orElseGet(() -> createNewPayment(order));

        orderStatusUseCase.markPaymentPending(order.orderId(), command.userId());

        // TODO: 실제 토스 연동 시 PaymentFacade에서 트랜잭션 밖으로 분리 예정, 지금은 Mock이라 보류
        paymentGateway.ready(order.orderNo(), order.totalPaymentAmount());

        // ToDo: bulk 도입 시 수정 예정
        String orderName = orderSnapshotQueryUseCase.getFirstOrderItemSnapshot(order.orderId())
                .map(OrderItemSnapshotView::productName)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        return PaymentReadyResult.from(order, payment, orderName, successUrl, failUrl);
    }

    public PaymentConfirmContext loadForConfirm(PaymentConfirmCommand command) {
        Payment payment = paymentRepository.findByOrderNo(command.tossOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getTotalAmount().equals(command.amount())) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        if (payment.getPaymentStatus() != PaymentStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        // ToDo: bulk 도입 시 수정 예정
        OrderItemSnapshotView item = orderSnapshotQueryUseCase.getFirstOrderItemSnapshot(payment.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        return new PaymentConfirmContext(payment.getId(), payment.getOrderId(), payment.getUserId(), item.orderItemId(), item.productId(), item.timeDealId());
    }

    @Transactional
    public PaymentConfirmResult applyApproved(PaymentConfirmContext context, PaymentConfirmCommand command, PaymentApproveResult approveResult) {
        orderStatusUseCase.markPaid(context.orderId(), context.userId(), approveResult.approvedAt());

        Payment payment = paymentRepository.findById(context.paymentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        payment.approve(command.paymentKey(), approveResult.paymentMethod(), approveResult.approvedAt(), approveResult.receiptUrl());

        PaymentTransaction transaction = PaymentTransaction.create(
                payment.getId(), TransactionType.APPROVE, command.amount(), command.idempotencyKey(), null
        );
        transaction.complete(
                TransactionStatus.SUCCESS, approveResult.pgTransactionKey(), payment.getBalanceAmount(), null, null, approveResult.approvedAt()
        );
        paymentTransactionRepository.save(transaction);

        return PaymentConfirmResult.from(payment, context.orderId(), command.tossOrderId(), OrderStatus.PAID);
    }

    // Mock은 항상 성공해 현재 호출되지 않음, 실제 토스 연동 시 PG 승인 실패 경로에서 사용
    @Transactional
    public void applyAborted(PaymentConfirmContext context) {
        orderStatusUseCase.revertToStockReserved(context.orderId(), context.userId());

        Payment payment = paymentRepository.findById(context.paymentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        payment.abort();
    }

    @Transactional
    public void markDeliveryPreparing(UUID orderId) {
        // ToDo: bulk 도입 시 수정 예정
        orderDeliveryGroupQueryUseCase.getDeliveryGroups(orderId).stream()
                .map(OrderDeliveryGroupView::deliveryGroupId)
                .forEach(orderDeliveryGroupStatusUseCase::markPreparing);

        deliveryCreateUseCase.createDeliveries(orderId);
    }

    @Transactional
    public void applyStockShortageCancel(PaymentConfirmContext context, PaymentCancelResult cancelResult) {
        orderCancelUseCase.cancelForStockShortage(context.orderId());

        Payment payment = paymentRepository.findById(context.paymentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        long cancelAmount = payment.getBalanceAmount();
        payment.applyCancellation(cancelAmount, cancelResult.canceledAt());

        PaymentTransaction transaction = PaymentTransaction.create(
                payment.getId(), TransactionType.CANCEL, cancelAmount, generateIdempotencyKey(), null
        );
        transaction.complete(
                TransactionStatus.SUCCESS, cancelResult.pgTransactionKey(), payment.getBalanceAmount(),
                CancelReasonCode.OUT_OF_STOCK.name(), null, cancelResult.canceledAt()
        );
        paymentTransactionRepository.save(transaction);
    }

    private Payment retryExistingPayment(Payment payment) {
        if (payment.getPaymentStatus() != PaymentStatus.ABORTED) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }

        payment.retry(generateIdempotencyKey());
        return payment;
    }

    private Payment createNewPayment(OrderSnapshotView order) {
        Payment payment = Payment.create(
                order.orderId(),
                order.orderNo(),
                order.userId(),
                order.totalPaymentAmount(),
                generateIdempotencyKey()
        );
        payment.start();
        return paymentRepository.save(payment);
    }

    private String generateIdempotencyKey() {
        return UUID.randomUUID().toString();
    }
}
