package com.parut.order.order.application;

import com.parut.order.order.application.port.in.SellerUnprocessedOrderQueryUseCase;
import com.parut.order.order.domain.DeliveryGroupStatus;
import com.parut.order.order.domain.OrderItemStatus;
import com.parut.order.order.infrastructure.persistence.OrderDeliveryGroupRepository;
import com.parut.order.order.infrastructure.persistence.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerUnprocessedOrderQueryService implements SellerUnprocessedOrderQueryUseCase {

    private final OrderDeliveryGroupRepository orderDeliveryGroupRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public boolean hasUnprocessedOrder(UUID sellerId) {
        return orderDeliveryGroupRepository.existsBySellerIdAndGroupStatus(sellerId, DeliveryGroupStatus.PREPARING)
                || orderItemRepository.existsBySellerIdAndItemStatus(sellerId, OrderItemStatus.REFUND_REQUESTED);
    }
}
