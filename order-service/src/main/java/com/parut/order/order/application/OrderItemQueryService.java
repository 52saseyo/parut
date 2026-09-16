package com.parut.order.order.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.order.application.port.in.OrderItemQueryUseCase;
import com.parut.order.order.application.port.in.dto.OrderItemView;
import com.parut.order.order.domain.Order;
import com.parut.order.order.domain.OrderDeliveryGroup;
import com.parut.order.order.domain.OrderItem;
import com.parut.order.order.infrastructure.persistence.OrderDeliveryGroupRepository;
import com.parut.order.order.infrastructure.persistence.OrderItemRepository;
import com.parut.order.order.infrastructure.persistence.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderItemQueryService implements OrderItemQueryUseCase {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final OrderDeliveryGroupRepository orderDeliveryGroupRepository;

    @Override
    public List<OrderItemView> getOrderItems(List<UUID> orderItemIds) {
        List<OrderItem> items = orderItemRepository.findAllById(orderItemIds);

        Map<UUID, UUID> buyerIdsByOrderId = orderRepository.findAllById(
                items.stream().map(OrderItem::getOrderId).distinct().toList()
        ).stream().collect(Collectors.toMap(Order::getId, Order::getUserId));

        Map<UUID, OrderDeliveryGroup> groupsById = orderDeliveryGroupRepository.findAllById(
                items.stream().map(OrderItem::getDeliveryGroupId).distinct().toList()
        ).stream().collect(Collectors.toMap(OrderDeliveryGroup::getId, group -> group));

        return items.stream()
                .map(item -> toView(item, buyerIdsByOrderId, groupsById))
                .toList();
    }

    private OrderItemView toView(OrderItem item, Map<UUID, UUID> buyerIdsByOrderId, Map<UUID, OrderDeliveryGroup> groupsById) {
        OrderDeliveryGroup group = groupsById.get(item.getDeliveryGroupId());
        return new OrderItemView(
                item.getId(),
                item.getOrderId(),
                buyerIdsByOrderId.get(item.getOrderId()),
                group.getSellerId(),
                item.getDeliveryGroupId(),
                item.getItemStatus(),
                group.getGroupStatus(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getConfirmedAt()
        );
    }
}
