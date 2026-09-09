package com.parut.order.order.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.order.application.port.in.OrderDeliveryGroupQueryUseCase;
import com.parut.order.order.application.port.in.dto.OrderDeliveryGroupView;
import com.parut.order.order.domain.OrderDeliveryGroup;
import com.parut.order.order.domain.OrderItemStatus;
import com.parut.order.order.infrastructure.persistence.OrderDeliveryGroupRepository;
import com.parut.order.order.infrastructure.persistence.OrderItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderDeliveryGroupQueryService implements OrderDeliveryGroupQueryUseCase {

    private final OrderDeliveryGroupRepository orderDeliveryGroupRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public List<OrderDeliveryGroupView> getDeliveryGroups(UUID orderId) {
        return orderDeliveryGroupRepository.findByOrderId(orderId).stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public Optional<OrderDeliveryGroupView> getDeliveryGroup(UUID deliveryGroupId) {
        return orderDeliveryGroupRepository.findById(deliveryGroupId)
                .map(this::toView);
    }

    private OrderDeliveryGroupView toView(OrderDeliveryGroup group) {
        int nonCanceledItemCount = orderItemRepository.countByDeliveryGroupIdAndItemStatus(
                group.getId(),
                OrderItemStatus.ORDERED
        );
        return new OrderDeliveryGroupView(group.getId(), group.getSellerId(), nonCanceledItemCount);
    }
}
