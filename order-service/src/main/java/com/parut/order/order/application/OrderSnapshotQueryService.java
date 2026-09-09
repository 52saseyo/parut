package com.parut.order.order.application;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.order.application.port.in.OrderSnapshotQueryUseCase;
import com.parut.order.order.application.port.in.dto.OrderItemSnapshotView;
import com.parut.order.order.application.port.in.dto.OrderSnapshotView;
import com.parut.order.order.infrastructure.persistence.OrderItemRepository;
import com.parut.order.order.infrastructure.persistence.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderSnapshotQueryService implements OrderSnapshotQueryUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public Optional<OrderSnapshotView> getOrderSnapshot(UUID orderId) {
        return orderRepository.findById(orderId).map(OrderSnapshotView::from);
    }

    @Override
    public Optional<OrderItemSnapshotView> getFirstOrderItemSnapshot(UUID orderId) {
        // ToDo: bulk 도입 시 아이템 목록 전체를 반환하도록 수정 필요
        return orderItemRepository.findByOrderId(orderId).stream()
                .findFirst()
                .map(OrderItemSnapshotView::from);
    }
}
