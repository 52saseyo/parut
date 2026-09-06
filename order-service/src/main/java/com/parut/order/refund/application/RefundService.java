package com.parut.order.refund.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.refund.application.dto.RefundOrderItemSnapshot;
import com.parut.order.refund.application.port.RefundOrderItemQueryPort;
import com.parut.order.refund.domain.Refund;
import com.parut.order.refund.domain.RefundStatus;
import com.parut.order.refund.infrastructure.persistence.RefundRepository;

import lombok.RequiredArgsConstructor;

/**
 * 환불 요청과 고객의 요청 취소를 처리한다.
 *
 * <p>환불 승인과 결제 취소는 Payment 계약이 확정된 뒤 연결한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {

    private final RefundRepository refundRepository;

    // NOTE: Order 구현이 들어오기 전까지는 조회 Port 없이도 애플리케이션이 기동되어야 한다.
    private final ObjectProvider<RefundOrderItemQueryPort> orderItemQueryPortProvider;

    @Transactional
    public Refund requestRefund(
            UUID orderItemId,
            UUID customerId,
            String reason
    ) {
        if (orderItemId == null || customerId == null || reason == null || reason.isBlank()
                || reason.length() > 500) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        RefundOrderItemSnapshot orderItem = requireOrderItemQueryPort()
                .findById(orderItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_ALLOWED));

        validateRequestable(orderItem, customerId);

        if (refundRepository.existsByOrderItemIdAndStatusNot(orderItemId, RefundStatus.CANCELED)) {
            throw new BusinessException(ErrorCode.REFUND_ALREADY_REQUESTED);
        }

        return refundRepository.save(Refund.request(
                orderItemId,
                orderItem.refundAmount(),
                reason,
                Instant.now()
        ));
    }

    @Transactional
    public Refund cancelRefund(
            UUID refundId,
            UUID customerId
    ) {
        if (refundId == null || customerId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));

        RefundOrderItemSnapshot orderItem = requireOrderItemQueryPort()
                .findById(refund.getOrderItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_CANCEL_NOT_ALLOWED));

        if (!customerId.equals(orderItem.customerId()) || refund.getStatus() != RefundStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.REFUND_CANCEL_NOT_ALLOWED);
        }

        refund.cancel(Instant.now());
        return refund;
    }

    private void validateRequestable(
            RefundOrderItemSnapshot orderItem,
            UUID customerId
    ) {
        if (!customerId.equals(orderItem.customerId())
                || !orderItem.delivered()
                || orderItem.confirmed()
                || orderItem.canceled()
                || orderItem.refundAmount() < 0) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED);
        }
    }

    private RefundOrderItemQueryPort requireOrderItemQueryPort() {
        RefundOrderItemQueryPort port = orderItemQueryPortProvider.getIfAvailable();
        if (port == null) {
            throw new IllegalStateException("Order 주문상품 조회 Port 구현이 필요합니다.");
        }
        return port;
    }
}
