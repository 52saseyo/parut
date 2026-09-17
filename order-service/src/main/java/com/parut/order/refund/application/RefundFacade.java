package com.parut.order.refund.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.payment.application.port.in.PaymentCancelUseCase;
import com.parut.order.payment.application.port.in.dto.PaymentCancelReceipt;
import com.parut.order.refund.application.dto.RefundApprovalContext;
import com.parut.order.refund.domain.Refund;

import lombok.RequiredArgsConstructor;

/**
 * 환불 승인에 필요한 검증, Payment 취소와 완료 상태 반영 순서를 조율한다.
 *
 * <p>PG 취소는 Refund의 DB 트랜잭션 밖에서 실행하고, 결제 원장과 환불 상태 반영은
 * {@link RefundService}의 트랜잭션에서 처리한다.
 */
@Component
@RequiredArgsConstructor
public class RefundFacade {

    private static final String CANCEL_REASON = "REFUND";

    private final RefundService refundService;
    private final PaymentCancelUseCase paymentCancelUseCase;

    public List<Refund> approveRefunds(List<UUID> refundIds, UUID sellerId) {
        RefundApprovalContext context = refundService.prepareApproval(refundIds, sellerId);

        PaymentCancelReceipt receipt = paymentCancelUseCase
                .cancelOnPg(context.orderId(), context.totalRefundAmount(), CANCEL_REASON)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS));

        return refundService.completeApproval(context, receipt);
    }
}
