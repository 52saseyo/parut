package com.parut.order.order.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.in.OrderItemRefundUseCase;
import com.parut.order.order.domain.DeliveryGroupStatus;
import com.parut.order.order.domain.OrderDeliveryGroup;
import com.parut.order.order.domain.OrderItem;
import com.parut.order.order.domain.OrderItemStatus;
import com.parut.order.order.infrastructure.persistence.OrderDeliveryGroupRepository;
import com.parut.order.order.infrastructure.persistence.OrderItemRepository;
import com.parut.order.settlement.application.port.in.SettlementCreateUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderItemRefundService implements OrderItemRefundUseCase {

    private final OrderItemRepository orderItemRepository;
    private final OrderDeliveryGroupRepository orderDeliveryGroupRepository;
    private final SettlementCreateUseCase settlementCreateUseCase;

    @Override
    @Transactional
    public void requestRefund(List<UUID> orderItemIds) {
        List<OrderItem> items = findAllOrThrow(orderItemIds);
        Map<UUID, OrderDeliveryGroup> groupsById = loadGroups(items);

        for (OrderItem item : items) {
            DeliveryGroupStatus groupStatus = groupsById.get(item.getDeliveryGroupId()).getGroupStatus();
            if (!item.isRefundable(groupStatus)) {
                throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
            }
        }
        items.forEach(OrderItem::requestRefund);
    }

    @Override
    @Transactional
    public void withdrawRefundRequest(List<UUID> orderItemIds) {
        List<OrderItem> items = findAllOrThrow(orderItemIds);
        verifyAllInStatus(items, OrderItemStatus.REFUND_REQUESTED);
        items.forEach(OrderItem::returnToOrdered);
    }

    @Override
    @Transactional
    public void applyRefundCompletion(List<UUID> orderItemIds) {
        List<OrderItem> items = findAllOrThrow(orderItemIds);
        verifyAllInStatus(items, OrderItemStatus.REFUND_REQUESTED);
        items.forEach(OrderItem::markRefunded);
    }

    @Override
    @Transactional
    public void rejectRefund(List<UUID> orderItemIds) {
        List<OrderItem> items = findAllOrThrow(orderItemIds);
        verifyAllInStatus(items, OrderItemStatus.REFUND_REQUESTED);
        Instant confirmedAt = Instant.now();
        // 환불 거절은 구매확정으로 전환한 뒤 같은 트랜잭션에서 정산을 생성한다.
        items.forEach(item -> {
            item.confirm(confirmedAt);
            settlementCreateUseCase.createSettlement(item.getId());
        });
    }

    private List<OrderItem> findAllOrThrow(List<UUID> orderItemIds) {
        List<OrderItem> items = orderItemRepository.findAllById(orderItemIds);
        if (items.size() != orderItemIds.size()) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND);
        }
        return items;
    }

    private void verifyAllInStatus(List<OrderItem> items, OrderItemStatus status) {
        boolean allMatch = items.stream().allMatch(item -> item.getItemStatus() == status);
        if (!allMatch) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
    }

    private Map<UUID, OrderDeliveryGroup> loadGroups(List<OrderItem> items) {
        List<UUID> groupIds = items.stream().map(OrderItem::getDeliveryGroupId).distinct().toList();
        return orderDeliveryGroupRepository.findAllById(groupIds).stream()
                .collect(Collectors.toMap(OrderDeliveryGroup::getId, group -> group));
    }
}
