package com.parut.order.order.application;

import com.parut.order.order.application.port.in.OrderSnapshotQueryUseCase;
import com.parut.order.order.application.port.in.dto.OrderItemSnapshotView;
import com.parut.order.order.application.port.in.dto.OrderSnapshotView;
import com.parut.order.order.infrastructure.persistence.OrderItemRepository;
import com.parut.order.order.infrastructure.persistence.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public List<OrderItemSnapshotView> getOrderItemSnapshots(UUID orderId) {
        return orderItemRepository.findByOrderId(orderId).stream()
                .map(OrderItemSnapshotView::from)
                .toList();
    }
}
