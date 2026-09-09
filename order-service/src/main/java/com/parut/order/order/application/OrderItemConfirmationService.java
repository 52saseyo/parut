package com.parut.order.order.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.domain.DeliveryGroupStatus;
import com.parut.order.order.domain.Order;
import com.parut.order.order.domain.OrderDeliveryGroup;
import com.parut.order.order.domain.OrderItem;
import com.parut.order.order.domain.OrderItemStatus;
import com.parut.order.order.infrastructure.persistence.OrderDeliveryGroupRepository;
import com.parut.order.order.infrastructure.persistence.OrderItemRepository;
import com.parut.order.order.infrastructure.persistence.OrderRepository;

import lombok.RequiredArgsConstructor;

/**
 * 주문상품의 구매확정 조건을 검증하고 상태 변경을 처리한다.
 *
 * <p>이미 확정된 주문상품은 기존 결과를 반환하며, 주문 전체 상태는 변경하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderItemConfirmationService {

    private final OrderRepository orderRepository;
    private final OrderDeliveryGroupRepository orderDeliveryGroupRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * 배송 완료된 본인 주문상품을 구매 확정한다.
     */
    @Transactional
    public OrderItem confirmOrderItem(UUID orderId, UUID orderItemId, UUID userId) {
        if (orderId == null || orderItemId == null || userId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));
        // 다른 주문의 상품도 존재 여부를 노출하지 않도록 동일한 NOT_FOUND로 처리한다.
        if (!orderItem.getOrderId().equals(orderId)) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND);
        }
        // 동일 요청이 반복되더라도 최초 구매확정 시각을 유지한다.
        if (orderItem.getItemStatus() == OrderItemStatus.CONFIRMED) {
            return orderItem;
        }

        OrderDeliveryGroup deliveryGroup = orderDeliveryGroupRepository.findById(orderItem.getDeliveryGroupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_DELIVERY_GROUP_NOT_FOUND));
        // Delivery가 배송을 완료하면 함께 변경되는 주문 배송 그룹의 완료 상태를 확인한다.
        if (deliveryGroup.getGroupStatus() != DeliveryGroupStatus.DELIVERED
                || orderItem.getItemStatus() != OrderItemStatus.ORDERED) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_CONFIRMATION_NOT_ALLOWED);
        }

        orderItem.confirm(Instant.now());
        return orderItem;
    }
}
