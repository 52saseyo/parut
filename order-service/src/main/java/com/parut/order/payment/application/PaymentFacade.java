package com.parut.order.payment.application;

import org.springframework.stereotype.Component;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.payment.application.dto.PaymentConfirmCommand;
import com.parut.order.payment.application.dto.PaymentConfirmContext;
import com.parut.order.payment.application.dto.PaymentConfirmResult;
import com.parut.order.payment.application.port.out.PaymentGateway;
import com.parut.order.payment.application.port.out.ProductStockConfirmClient;
import com.parut.order.payment.application.port.out.dto.PaymentApproveResult;
import com.parut.order.payment.application.port.out.dto.PaymentCancelResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결제 승인의 전체 흐름을 조율합니다.
 *
 * <p>PG, Product Feign 호출은 트랜잭션 밖에서 수행하고,
 * {@link PaymentService}의 짧은 단위 트랜잭션을 순차 호출합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;
    private final ProductStockConfirmClient productStockConfirmClient;

    public PaymentConfirmResult confirm(PaymentConfirmCommand command) {
        PaymentConfirmContext context = paymentService.loadForConfirm(command);

        PaymentApproveResult approveResult;
        try {
            approveResult = paymentGateway.approve(command.paymentKey(), command.tossOrderId(), command.amount(), command.idempotencyKey());
        } catch (RuntimeException e) {
            log.warn("[PaymentFacade] PG 승인 실패. paymentId={}", context.paymentId(), e);
            paymentService.applyAborted(context);
            throw new BusinessException(ErrorCode.PG_APPROVE_FAILED);
        }

        PaymentConfirmResult result = paymentService.applyApproved(context, command, approveResult);

        try {
            productStockConfirmClient.confirmStock(context.productId(), context.orderId(), context.orderItemId());
        } catch (BusinessException e) {
            if (e.getErrorCode() != ErrorCode.STOCK_SHORTAGE) {
                throw e;
            }
            compensateStockShortage(context, command, result);
            throw e;
        }

        paymentService.markDeliveryPreparing(context.orderId());

        return result;
    }

    private void compensateStockShortage(PaymentConfirmContext context, PaymentConfirmCommand command, PaymentConfirmResult result) {
        PaymentCancelResult cancelResult;
        try {
            cancelResult = paymentGateway.cancel(command.paymentKey(), result.balanceAmount(), "OUT_OF_STOCK");
        } catch (RuntimeException e) {
            log.error("[PaymentFacade] PG 취소 실패. 수동 대응 필요 paymentId={}", context.paymentId(), e);
            throw new BusinessException(ErrorCode.PG_CANCEL_FAILED);
        }

        paymentService.applyStockShortageCancel(context, cancelResult);
    }
}
