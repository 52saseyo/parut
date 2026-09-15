package com.parut.order.refund.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.payment.application.port.in.PaymentCancelUseCase;
import com.parut.order.payment.application.port.in.dto.PaymentCancelCommand;
import com.parut.order.payment.application.port.in.dto.PaymentCancelView;
import com.parut.order.refund.application.dto.RefundApprovalContext;
import com.parut.order.refund.domain.Refund;

import lombok.RequiredArgsConstructor;

/** 환불 승인에 필요한 Refund 검증, Payment 취소와 완료 반영 순서를 조율한다. */
@Component
@RequiredArgsConstructor
public class RefundFacade {

    private static final String CANCEL_REASON = "REFUND";

    private final RefundService refundService;
    private final PaymentCancelUseCase paymentCancelUseCase;

    public List<Refund> approveRefunds(
            List<UUID> refundIds,
            UUID sellerId,
            String cancelRequestId
    ) {
        if (cancelRequestId == null || cancelRequestId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        RefundApprovalContext context = refundService.prepareApproval(refundIds, sellerId);

        PaymentCancelView paymentCancel = paymentCancelUseCase.cancel(
                new PaymentCancelCommand(
                        context.orderId(),
                        cancelRequestId,
                        context.totalRefundAmount(),
                        CANCEL_REASON
                )
        );

        return refundService.completeApproval(context, paymentCancel);
    }
}
