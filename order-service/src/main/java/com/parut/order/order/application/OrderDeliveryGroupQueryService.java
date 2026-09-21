package com.parut.order.order.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.in.OrderDeliveryGroupQueryUseCase;
import com.parut.order.order.application.port.in.dto.OrderDeliveryGroupView;
import com.parut.order.order.domain.OrderDeliveryGroup;
import com.parut.order.order.domain.OrderItemStatus;
import com.parut.order.order.infrastructure.persistence.OrderDeliveryGroupRepository;
import com.parut.order.order.infrastructure.persistence.OrderItemRepository;
import com.parut.order.order.infrastructure.persistence.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderDeliveryGroupQueryService implements OrderDeliveryGroupQueryUseCase {

    private final OrderDeliveryGroupRepository orderDeliveryGroupRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    @Override
    public List<OrderDeliveryGroupView> getDeliveryGroups(UUID orderId) {
        UUID customerId = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND))
                .getUserId();
        return orderDeliveryGroupRepository.findByOrderId(orderId).stream()
                .map(group -> toView(group, customerId))
                .toList();
    }

    @Override
    public Optional<OrderDeliveryGroupView> getDeliveryGroup(UUID deliveryGroupId) {
        return orderDeliveryGroupRepository.findById(deliveryGroupId)
                .map(this::toView);
    }

    private OrderDeliveryGroupView toView(OrderDeliveryGroup group) {
        UUID customerId = orderRepository.findById(group.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND))
                .getUserId();
        return toView(group, customerId);
    }

    private OrderDeliveryGroupView toView(OrderDeliveryGroup group, UUID customerId) {
        int shippableItemCount = orderItemRepository.countByDeliveryGroupIdAndItemStatus(
                group.getId(),
                OrderItemStatus.ORDERED
        );
        return new OrderDeliveryGroupView(
                group.getId(),
                group.getOrderId(),
                customerId,
                group.getSellerId(),
                shippableItemCount
        );
    }
}
