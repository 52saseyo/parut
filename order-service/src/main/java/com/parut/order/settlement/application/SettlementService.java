package com.parut.order.settlement.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.in.OrderItemQueryUseCase;
import com.parut.order.order.application.port.in.dto.OrderItemView;
import com.parut.order.order.domain.OrderItemStatus;
import com.parut.order.settlement.application.port.in.SettlementCreateUseCase;
import com.parut.order.settlement.domain.Settlement;
import com.parut.order.settlement.domain.SettlementStatus;
import com.parut.order.settlement.infrastructure.persistence.SettlementRepository;

import lombok.RequiredArgsConstructor;

/**
 * 주문상품 단위 정산의 생성, 조회와 관리자 완료 처리를 조율한다.
 *
 * <p>정산 생성은 구매확정 트랜잭션에 참여하며, 다건 완료는 모든 대상을 검증한 뒤 한 번에 상태를 변경한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService implements SettlementCreateUseCase {

    private final SettlementRepository settlementRepository;
    private final OrderItemQueryUseCase orderItemQueryUseCase;

    /** 구매확정과 같은 트랜잭션에서 주문상품 단위 대기 정산을 생성한다. */
    @Override
    @Transactional
    public void createSettlement(UUID orderItemId) {
        // Order가 확정한 주문상품의 상태와 금액 원본을 조회한다.
        OrderItemView orderItem = getConfirmedOrderItem(orderItemId);

        // 이번 범위에서는 배송비와 수수료를 제외한 상품 금액만 정산한다.
        long salesAmount = Math.multiplyExact(orderItem.unitPrice(), orderItem.quantity());
        settlementRepository.save(Settlement.create(
                orderItemId,
                orderItem.sellerId(),
                salesAmount,
                salesAmount,
                orderItem.confirmedAt()
        ));
    }

    public SettlementPage getSellerSettlements(
            UUID sellerId, SettlementStatus status, Instant cursor, UUID cursorId, int size) {
        return page(settlementRepository.findSellerSettlements(
                sellerId, status, cursor, cursorId, PageRequest.of(0, size + 1)), size);
    }

    public SettlementPage getAdminSettlements(
            UUID sellerId, Instant cursor, UUID cursorId, int size) {
        return page(settlementRepository.findAdminSettlements(
                sellerId, SettlementStatus.PENDING, cursor, cursorId, PageRequest.of(0, size + 1)), size);
    }

    @Transactional
    public List<Settlement> completeSettlements(List<UUID> settlementIds, UUID processedBy, Instant completionTime) {
        if (settlementIds == null || settlementIds.isEmpty() || settlementIds.size() > 50
                || settlementIds.stream().distinct().count() != settlementIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<Settlement> settlements = settlementRepository.findAllById(settlementIds);
        if (settlements.size() != settlementIds.size()) {
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND);
        }

        // 일부 정산만 완료되는 상황을 막기 위해 모든 대상을 검증한 뒤 상태를 변경한다.
        settlements.forEach(settlement -> {
            if (settlement.getStatus() != SettlementStatus.PENDING) {
                throw new BusinessException(ErrorCode.SETTLEMENT_ALREADY_COMPLETED);
            }
            if (completionTime.isBefore(settlement.getEligibleAt())) {
                throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
            }
        });
        settlements.forEach(settlement -> settlement.complete(completionTime, processedBy));
        return settlements;
    }

    private SettlementPage page(List<Settlement> settlements, int size) {
        // 요청 크기보다 한 건 더 조회해 별도 count 쿼리 없이 다음 페이지 존재 여부를 판단한다.
        boolean hasNext = settlements.size() > size;
        List<Settlement> content = hasNext ? settlements.subList(0, size) : settlements;
        Settlement last = content.isEmpty() ? null : content.get(content.size() - 1);
        return new SettlementPage(content,
                last == null ? null : last.getCreatedAt(),
                last == null ? null : last.getId(), hasNext);
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
