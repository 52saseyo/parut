package com.parut.order.order.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.in.OrderCancelUseCase;
import com.parut.order.order.domain.CancelReasonCode;
import com.parut.order.order.domain.CanceledByType;
import com.parut.order.order.domain.Order;
import com.parut.order.order.domain.OrderCancel;
import com.parut.order.order.domain.OrderDeliveryGroup;
import com.parut.order.order.domain.OrderItem;
import com.parut.order.order.domain.OrderStatus;
import com.parut.order.order.domain.OrderStatusHistory;
import com.parut.order.order.infrastructure.persistence.OrderCancelRepository;
import com.parut.order.order.infrastructure.persistence.OrderDeliveryGroupRepository;
import com.parut.order.order.infrastructure.persistence.OrderItemRepository;
import com.parut.order.order.infrastructure.persistence.OrderRepository;
import com.parut.order.order.infrastructure.persistence.OrderStatusHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderCancelService implements OrderCancelUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderDeliveryGroupRepository orderDeliveryGroupRepository;
    private final OrderCancelRepository orderCancelRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Value("${app.system-account-id}")
    private String systemAccountId;

    @Override
    public void cancelForStockShortage(UUID orderId) {
        // 락 획득 순서: 주문 -> 배송그룹 -> 아이템
        Order order = getOrder(orderId);
        // ToDo: bulk 도입 시 수정 필요
        OrderDeliveryGroup group = orderDeliveryGroupRepository.findByOrderId(orderId).get(0);
        OrderItem item = orderItemRepository.findByOrderId(orderId).get(0);

        long cancelProductAmount = item.getUnitPrice() * item.getQuantity();
        long cancelDeliveryFee = group.getDeliveryFee(); // 그룹에 ORDERED 아이템이 안 남으므로 전액 환불

        OrderCancel orderCancel = orderCancelRepository.save(
                OrderCancel.create(
                        orderId,
                        CancelReasonCode.OUT_OF_STOCK,
                        null,
                        CanceledByType.SYSTEM,
                        systemAccountId,
                        cancelProductAmount,
                        cancelDeliveryFee,
                        true
                )
        );

        Instant canceledAt = Instant.now();
        group.cancel(canceledAt);
        item.cancel(orderCancel.getId());
        order.cancel();

        orderStatusHistoryRepository.save(
                OrderStatusHistory.record(orderId, OrderStatus.PAID, OrderStatus.CANCELED, "재고 확정 실패", systemAccountId)
        );
    }

    private Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }
}
