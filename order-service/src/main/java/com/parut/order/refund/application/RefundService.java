package com.parut.order.refund.application;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;

/**
 * 환불 요청과 고객의 요청 취소, 판매자 거절을 처리한다.
 *
 * <p>주문상품 조회와 상태 변경은 Order 포트를 통해 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {

    private final RefundRepository refundRepository;
    private final DeliveryCompletionQueryUseCase deliveryCompletionQueryUseCase;
    private final OrderItemQueryUseCase orderItemQueryUseCase;
    private final OrderItemRefundUseCase orderItemRefundUseCase;

    public RefundApprovalContext prepareApproval(
            List<UUID> refundIds,
            UUID sellerId
    ) {
        if (sellerId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<Refund> refunds = getRequestedRefunds(refundIds);
        List<OrderItemDetailView> orderItems = getOrderItems(refunds);
        long totalRefundAmount = validateAndCalculateRefundAmount(refunds, orderItems, sellerId);

        List<UUID> orderItemIds = orderItems.stream()
                .map(OrderItemDetailView::orderItemId)
                .toList();

        return new RefundApprovalContext(
                orderItems.get(0).orderId(),
                sellerId,
                List.copyOf(refundIds),
                orderItemIds,
                totalRefundAmount
        );
    }

    /** Payment 취소 성공 후 Order와 Refund의 완료 상태를 한 트랜잭션으로 반영한다. */
    @Transactional
    public List<Refund> completeApproval(
            RefundApprovalContext context,
            PaymentCancelView paymentCancel
    ) {
        if (context == null || paymentCancel == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (paymentCancel.canceledAmount() != context.totalRefundAmount()) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        List<Refund> refunds = getRequestedRefunds(context.refundIds());

        orderItemRefundUseCase.markRefunded(context.orderItemIds());
        refunds.forEach(refund -> refund.approve(paymentCancel.canceledAt(), context.sellerId()));

        return refunds;
    }

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

        OrderItemDetailView orderItem = getOrderItem(orderItemId);
        if (!customerId.equals(orderItem.buyerId())
                || orderItem.itemStatus() != OrderItemStatus.ORDERED
                || orderItem.groupStatus() != DeliveryGroupStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED);
        }

        // 환불 기한은 Delivery가 기록한 실제 배송 완료 시각으로 판단한다.
        Instant deliveredAt = deliveryCompletionQueryUseCase
                .getDeliveredAt(orderItem.deliveryGroupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_ALLOWED));

        Instant now = Instant.now();
        if (now.isBefore(deliveredAt) || now.isAfter(deliveredAt.plus(Duration.ofDays(7)))) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED);
        }

        if (refundRepository.existsByOrderItemIdAndStatusNot(orderItemId, RefundStatus.CANCELED)) {
            throw new BusinessException(ErrorCode.REFUND_ALREADY_REQUESTED);
        }

        long refundAmount = Math.multiplyExact(orderItem.unitPrice(), orderItem.quantity());

        orderItemRefundUseCase.markRefundRequested(orderItemId);
        return refundRepository.save(Refund.request(orderItemId, refundAmount, reason, now));
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

        OrderItemDetailView orderItem = getOrderItem(refund.getOrderItemId());
        if (!customerId.equals(orderItem.buyerId()) || refund.getStatus() != RefundStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.REFUND_CANCEL_NOT_ALLOWED);
        }

        orderItemRefundUseCase.cancelRefundRequest(refund.getOrderItemId());
        refund.cancel(Instant.now());
        return refund;
    }

    @Transactional
    public Refund rejectRefund(
            UUID refundId,
            UUID sellerId,
            String rejectionReason
    ) {
        if (refundId == null || sellerId == null || rejectionReason == null || rejectionReason.isBlank()
                || rejectionReason.length() > 500) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));

        if (!sellerId.equals(getOrderItem(refund.getOrderItemId()).sellerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.REFUND_ALREADY_PROCESSED);
        }

        orderItemRefundUseCase.confirmRejectedRefund(refund.getOrderItemId());
        refund.reject(rejectionReason, Instant.now(), sellerId);
        return refund;
    }

    private List<Refund> getRequestedRefunds(List<UUID> refundIds) {
        if (refundIds == null
                || refundIds.isEmpty()
                || refundIds.stream().anyMatch(Objects::isNull)
                || refundIds.stream().distinct().count() != refundIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<Refund> refunds = refundRepository.findAllById(refundIds);

        if (refunds.size() != refundIds.size()) {
            throw new BusinessException(ErrorCode.REFUND_NOT_FOUND);
        }

        if (refunds.stream().anyMatch(refund -> refund.getStatus() != RefundStatus.REQUESTED)) {
            throw new BusinessException(ErrorCode.REFUND_ALREADY_PROCESSED);
        }

        return refunds;
    }

    private List<OrderItemDetailView> getOrderItems(List<Refund> refunds) {
        List<UUID> orderItemIds = refunds.stream()
                .map(Refund::getOrderItemId)
                .toList();

        List<OrderItemDetailView> orderItems = orderItemQueryUseCase.getOrderItems(orderItemIds);

        Set<UUID> returnedOrderItemIds = orderItems.stream()
                .map(OrderItemDetailView::orderItemId)
                .collect(Collectors.toSet());

        if (orderItems.size() != orderItemIds.size()
                || !returnedOrderItemIds.containsAll(orderItemIds)) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND);
        }

        return orderItems;
    }

    private long validateAndCalculateRefundAmount(
            List<Refund> refunds,
            List<OrderItemDetailView> orderItems,
            UUID sellerId
    ) {
        UUID orderId = orderItems.get(0).orderId();

        Map<UUID, Refund> refundByOrderItemId = refunds.stream()
                .collect(Collectors.toMap(
                        Refund::getOrderItemId,
                        Function.identity()
                ));

        long totalRefundAmount = 0L;

        for (OrderItemDetailView orderItem : orderItems) {
            if (!sellerId.equals(orderItem.sellerId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }

            if (!orderId.equals(orderItem.orderId())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }

            if (orderItem.itemStatus() != OrderItemStatus.REFUND_REQUESTED) {
                throw new BusinessException(ErrorCode.REFUND_ALREADY_PROCESSED);
            }

            Refund refund = refundByOrderItemId.get(orderItem.orderItemId());

            long expectedRefundAmount = Math.multiplyExact(
                    orderItem.unitPrice(),
                    orderItem.quantity()
            );

            if (!Objects.equals(refund.getRefundAmount(), expectedRefundAmount)) {
                throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED);
            }

            totalRefundAmount = Math.addExact(
                    totalRefundAmount,
                    expectedRefundAmount
            );
        }

        return totalRefundAmount;
    }

    private OrderItemDetailView getOrderItem(UUID orderItemId) {
        return orderItemQueryUseCase
                .getOrderItems(List.of(orderItemId))
                .stream()
                .filter(item -> orderItemId.equals(item.orderItemId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));
    }

}
