package com.parut.order.order.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.in.OrderDeliveryGroupStatusUseCase;
import com.parut.order.order.domain.DeliveryGroupStatus;
import com.parut.order.order.domain.OrderDeliveryGroup;
import com.parut.order.order.infrastructure.persistence.OrderDeliveryGroupRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderDeliveryGroupStatusService implements OrderDeliveryGroupStatusUseCase {

    private final OrderDeliveryGroupRepository orderDeliveryGroupRepository;

    @Override
    public void markShipped(UUID deliveryGroupId) {
        OrderDeliveryGroup group = getDeliveryGroup(deliveryGroupId);
        if (group.getGroupStatus() != DeliveryGroupStatus.PREPARING) {
            throw new BusinessException(ErrorCode.ORDER_DELIVERY_GROUP_INVALID_STATUS_TRANSITION);
        }

        group.markShipped();
    }

    @Override
    public void markDelivered(UUID deliveryGroupId) {
        OrderDeliveryGroup group = getDeliveryGroup(deliveryGroupId);
        if (group.getGroupStatus() != DeliveryGroupStatus.SHIPPED) {
            throw new BusinessException(ErrorCode.ORDER_DELIVERY_GROUP_INVALID_STATUS_TRANSITION);
        }

        group.markDelivered();
    }

    private OrderDeliveryGroup getDeliveryGroup(UUID deliveryGroupId) {
        return orderDeliveryGroupRepository.findById(deliveryGroupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_DELIVERY_GROUP_NOT_FOUND));
    }
}
