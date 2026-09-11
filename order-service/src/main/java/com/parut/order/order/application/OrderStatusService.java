package com.parut.order.order.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.in.OrderStatusUseCase;
import com.parut.order.order.domain.Order;
import com.parut.order.order.domain.OrderStatus;
import com.parut.order.order.domain.OrderStatusHistory;
import com.parut.order.order.infrastructure.persistence.OrderRepository;
import com.parut.order.order.infrastructure.persistence.OrderStatusHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderStatusService implements OrderStatusUseCase {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Override
    public void markPaymentPending(UUID orderId, UUID actorId) {
        Order order = getOrder(orderId);
        order.markPaymentPending();
        recordHistory(orderId, OrderStatus.STOCK_RESERVED, OrderStatus.PAYMENT_PENDING, "결제 준비", actorId);
    }

    @Override
    public void markPaid(UUID orderId, UUID actorId, Instant paidAt) {
        Order order = getOrder(orderId);
        order.markPaid(paidAt);
        recordHistory(orderId, OrderStatus.PAYMENT_PENDING, OrderStatus.PAID, "결제 승인", actorId);
    }

    @Override
    public void revertToStockReserved(UUID orderId, UUID actorId) {
        Order order = getOrder(orderId);
        order.revertToStockReserved();
        recordHistory(orderId, OrderStatus.PAYMENT_PENDING, OrderStatus.STOCK_RESERVED, "PG 승인 실패", actorId);
    }

    private Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void recordHistory(UUID orderId, OrderStatus from, OrderStatus to, String reason, UUID actorId) {
        orderStatusHistoryRepository.save(OrderStatusHistory.record(orderId, from, to, reason, actorId.toString()));
    }
}
