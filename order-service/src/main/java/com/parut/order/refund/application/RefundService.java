package com.parut.order.refund.application;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.delivery.application.port.in.DeliveryCompletionQueryUseCase;
import com.parut.order.global.auth.UserRole;
import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.in.OrderItemQueryUseCase;
import com.parut.order.order.application.port.in.OrderItemRefundUseCase;
import com.parut.order.order.application.port.in.dto.OrderItemView;
import com.parut.order.order.domain.DeliveryGroupStatus;
import com.parut.order.order.domain.OrderItemStatus;
import com.parut.order.payment.application.port.in.PaymentCancelUseCase;
import com.parut.order.payment.application.port.in.dto.PaymentCancelReceipt;
import com.parut.order.refund.application.dto.RefundApprovalContext;
import com.parut.order.refund.domain.Refund;
import com.parut.order.refund.domain.RefundStatus;
import com.parut.order.refund.infrastructure.persistence.RefundRepository;

import lombok.RequiredArgsConstructor;

/**
 * 환불 요청과 조회, 고객의 요청 취소, 판매자의 승인 및 거절을 처리한다.
 *
 * <p>주문상품 조회와 상태 변경은 Order 포트로, 배송 완료 시각 조회는 Delivery 포트로 처리한다.
 * 외부 Payment 취소를 포함한 승인 순서는 {@link RefundFacade}가 조율한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {

    private final RefundRepository refundRepository;
    private final DeliveryCompletionQueryUseCase deliveryCompletionQueryUseCase;
    private final OrderItemQueryUseCase orderItemQueryUseCase;
    private final OrderItemRefundUseCase orderItemRefundUseCase;
    private final PaymentCancelUseCase paymentCancelUseCase;

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

        OrderItemView orderItem = getOrderItem(orderItemId);
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

        orderItemRefundUseCase.requestRefund(List.of(orderItemId));
        return refundRepository.save(Refund.request(
                orderItemId,
                orderItem.buyerId(),
                orderItem.sellerId(),
                refundAmount,
                reason,
                now
        ));
    }

    /** 고객과 판매자가 환불 요청 당시 소유자 범위에서 환불 한 건을 조회한다. */
    public Refund getRefund(UUID refundId, UUID requesterId, UserRole requesterRole) {
        if (refundId == null || requesterId == null || requesterRole == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));

        if (!isOwnedBy(refund, requesterId, requesterRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return refund;
    }

    /**
     * 고객과 판매자의 소유자 스냅샷을 기준으로 환불 목록을 커서 조회한다.
     * 상태를 생략하면 소유자 범위의 모든 환불 상태를 반환한다.
     */
    public RefundPage getRefunds(
            UUID requesterId,
            UserRole requesterRole,
            RefundStatus status,
            String cursor,
            UUID cursorId,
            int size
    ) {
        validateQuery(requesterId, requesterRole, cursor, cursorId, size);
        Instant cursorTime = parseCursor(cursor);

        PageRequest pageable = PageRequest.of(0, size + 1);
        List<Refund> refunds = switch (requesterRole) {
            case CUSTOMER -> refundRepository.findCustomerRefunds(
                    requesterId, status, cursorTime, cursorId, pageable);
            case SELLER -> refundRepository.findSellerRefunds(
                    requesterId, status, cursorTime, cursorId, pageable);
            default -> throw new BusinessException(ErrorCode.FORBIDDEN);
        };

        boolean hasNext = refunds.size() > size;
        List<Refund> content = hasNext ? refunds.subList(0, size) : refunds;
        if (!hasNext) {
            return new RefundPage(content, null, null, false);
        }

        Refund lastRefund = content.getLast();
        return new RefundPage(
                content,
                lastRefund.getCreatedAt().toString(),
                lastRefund.getId(),
                true
        );
    }

    /**
     * 관리자가 전체 환불을 상태 조건으로 조회한다.
     * 관리 화면에 필요한 전체 건수와 페이지 번호를 제공하기 위해 오프셋 페이지를 사용한다.
     */
    public Page<Refund> getAdminRefunds(RefundStatus status, Pageable pageable) {
        if (status == null) {
            return refundRepository.findAll(pageable);
        }
        return refundRepository.findByStatus(status, pageable);
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

        OrderItemView orderItem = getOrderItem(refund.getOrderItemId());
        if (!customerId.equals(orderItem.buyerId()) || refund.getStatus() != RefundStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.REFUND_CANCEL_NOT_ALLOWED);
        }

        orderItemRefundUseCase.withdrawRefundRequest(List.of(refund.getOrderItemId()));
        refund.cancel(Instant.now());
        return refund;
    }

    /**
     * Payment 취소 전에 선택한 환불 요청의 판매자, 주문, 주문상품 상태와 금액을 검증한다.
     * 이 단계에서는 상태를 변경하지 않는다.
     */
    public RefundApprovalContext prepareApproval(
            List<UUID> refundIds,
            UUID sellerId
    ) {
        if (sellerId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<Refund> refunds = getRequestedRefunds(refundIds);
        List<OrderItemView> orderItems = getOrderItems(refunds);
        long totalRefundAmount = validateAndCalculateRefundAmount(refunds, orderItems, sellerId);

        List<UUID> orderItemIds = orderItems.stream()
                .map(OrderItemView::orderItemId)
                .toList();

        return new RefundApprovalContext(
                orderItems.get(0).orderId(),
                sellerId,
                List.copyOf(refundIds),
                orderItemIds,
                totalRefundAmount
        );
    }

    /** Payment 취소 성공 후 Order 주문상품과 Refund의 완료 상태를 한 트랜잭션으로 반영한다. */
    @Transactional
    public List<Refund> completeApproval(
            RefundApprovalContext context,
            PaymentCancelReceipt receipt
    ) {
        if (context == null || receipt == null || receipt.canceledAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (receipt.cancelAmount() != context.totalRefundAmount()) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        List<Refund> refunds = getRequestedRefunds(context.refundIds());

        orderItemRefundUseCase.applyRefundCompletion(context.orderItemIds());
        paymentCancelUseCase.applyCancellation(context.orderId(), receipt);
        refunds.forEach(refund -> refund.approve(receipt.canceledAt(), context.sellerId()));

        return refunds;
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

        orderItemRefundUseCase.rejectRefund(List.of(refund.getOrderItemId()));
        refund.reject(rejectionReason, Instant.now(), sellerId);
        return refund;
    }

    private OrderItemView getOrderItem(UUID orderItemId) {
        return orderItemQueryUseCase
                .getOrderItems(List.of(orderItemId))
                .stream()
                .filter(item -> orderItemId.equals(item.orderItemId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));
    }

    private boolean isOwnedBy(Refund refund, UUID requesterId, UserRole requesterRole) {
        return switch (requesterRole) {
            case CUSTOMER -> requesterId.equals(refund.getCustomerId());
            case SELLER -> requesterId.equals(refund.getSellerId());
            default -> false;
        };
    }

    private void validateQuery(
            UUID requesterId,
            UserRole requesterRole,
            String cursor,
            UUID cursorId,
            int size
    ) {
        boolean hasOnlyOneCursorValue = (cursor == null) != (cursorId == null);
        if (requesterId == null || requesterRole == null || hasOnlyOneCursorValue) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (size != 10 && size != 30 && size != 50) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
        }
    }

    private Instant parseCursor(String cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            return Instant.parse(cursor);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
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

    private List<OrderItemView> getOrderItems(List<Refund> refunds) {
        List<UUID> orderItemIds = refunds.stream()
                .map(Refund::getOrderItemId)
                .toList();

        List<OrderItemView> orderItems = orderItemQueryUseCase.getOrderItems(orderItemIds);

        Set<UUID> returnedOrderItemIds = orderItems.stream()
                .map(OrderItemView::orderItemId)
                .collect(Collectors.toSet());

        if (orderItems.size() != orderItemIds.size()
                || !returnedOrderItemIds.containsAll(orderItemIds)) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND);
        }

        return orderItems;
    }

    private long validateAndCalculateRefundAmount(
            List<Refund> refunds,
            List<OrderItemView> orderItems,
            UUID sellerId
    ) {
        UUID orderId = orderItems.get(0).orderId();

        Map<UUID, Refund> refundByOrderItemId = refunds.stream()
                .collect(Collectors.toMap(
                        Refund::getOrderItemId,
                        Function.identity()
                ));

        long totalRefundAmount = 0L;

        for (OrderItemView orderItem : orderItems) {
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

}
