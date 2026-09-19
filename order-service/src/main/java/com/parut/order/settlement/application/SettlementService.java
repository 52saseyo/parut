package com.parut.order.settlement.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.in.OrderItemQueryUseCase;
import com.parut.order.order.application.port.in.dto.OrderItemView;
import com.parut.order.order.domain.OrderItemStatus;
import com.parut.order.settlement.application.port.in.SettlementCreateUseCase;
import com.parut.order.settlement.domain.Settlement;
import com.parut.order.settlement.infrastructure.persistence.SettlementRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SettlementService implements SettlementCreateUseCase {

    private final SettlementRepository settlementRepository;
    private final OrderItemQueryUseCase orderItemQueryUseCase;

    /** 구매확정과 같은 트랜잭션에서 주문상품 단위 대기 정산을 생성한다. */
    @Override
    public void createSettlement(UUID orderItemId) {
        // Order가 확정한 주문상품의 상태와 금액 원본을 조회한다.
        OrderItemView orderItem = getConfirmedOrderItem(orderItemId);

        // 이번 범위에서는 배송비와 수수료를 제외한 상품 금액만 정산한다.
        long salesAmount = Math.multiplyExact(orderItem.unitPrice(), orderItem.quantity());
        settlementRepository.save(Settlement.create(
                orderItemId,
                salesAmount,
                salesAmount,
                orderItem.confirmedAt()
        ));
    }

    private OrderItemView getConfirmedOrderItem(UUID orderItemId) {
        if (orderItemId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<OrderItemView> orderItems = orderItemQueryUseCase.getOrderItems(List.of(orderItemId));
        // 조회 포트는 존재하는 항목만 반환하므로 요청 ID와 결과를 대조한다.
        if (orderItems.size() != 1 || !orderItemId.equals(orderItems.get(0).orderItemId())) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND);
        }

        OrderItemView orderItem = orderItems.get(0);
        // 구매확정 시각이 기록된 확정 상품만 정산 대상으로 인정한다.
        if (orderItem.itemStatus() != OrderItemStatus.CONFIRMED || orderItem.confirmedAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        return orderItem;
    }
}
