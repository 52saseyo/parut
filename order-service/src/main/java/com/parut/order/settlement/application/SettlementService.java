package com.parut.order.settlement.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
import lombok.extern.slf4j.Slf4j;

/**
 * 주문상품 단위 정산의 생성, 조회와 관리자 완료 처리를 조율한다.
 *
 * <p>정산 생성은 구매확정 트랜잭션에 참여하며, 다건 완료는 정산 한 건씩 독립 트랜잭션으로 처리하는 조율자로 동작한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService implements SettlementCreateUseCase {

    private static final int MAX_COMPLETION_SIZE = 50;

    private final SettlementRepository settlementRepository;
    private final OrderItemQueryUseCase orderItemQueryUseCase;
    private final SettlementCompletionProcessor settlementCompletionProcessor;

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

    /** 관리자가 정산 대기 목록과 완료 이력을 같은 목록에서 조회한다. */
    public Page<Settlement> getAdminSettlements(
            UUID sellerId, SettlementStatus status, Pageable pageable) {
        return settlementRepository.findAdminSettlements(sellerId, status, pageable);
    }

    /**
     * 정산 ID 목록을 요청 순서대로 한 건씩 완료하고 항목별 결과를 모아 반환한다.
     *
     * <p>각 정산은 {@link SettlementCompletionProcessor}의 새 트랜잭션에서 커밋되므로 뒤 항목의 실패가 앞의 완료를
     * 되돌리지 않는다. 조율 자체는 트랜잭션을 열지 않아 완료된 정산이 호출자의 트랜잭션에 묶이지 않는다.
     * 예상하지 못한 시스템 오류는 항목 실패로 바꾸지 않고 공통 예외 처리로 전파한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<SettlementCompletionResult> completeSettlements(
            List<UUID> settlementIds, UUID processedBy, Instant completionTime) {
        validateCompletionRequest(settlementIds, processedBy, completionTime);

        List<SettlementCompletionResult> results = new ArrayList<>();
        for (UUID settlementId : settlementIds) {
            try {
                results.add(SettlementCompletionResult.success(
                        settlementCompletionProcessor.completeOne(settlementId, processedBy, completionTime)));
            } catch (BusinessException e) {
                results.add(failure(settlementId, e.getErrorCode()));
            } catch (OptimisticLockingFailureException e) {
                // 낙관적 락 충돌은 Processor 본문이 아니라 프록시의 커밋 시점에도 발생한다.
                results.add(failure(settlementId, ErrorCode.CONCURRENT_MODIFICATION));
            }
        }
        return List.copyOf(results);
    }

    private void validateCompletionRequest(List<UUID> settlementIds, UUID processedBy, Instant completionTime) {
        if (settlementIds == null || settlementIds.isEmpty() || settlementIds.size() > MAX_COMPLETION_SIZE
                || settlementIds.stream().anyMatch(Objects::isNull)
                || settlementIds.stream().distinct().count() != settlementIds.size()
                || processedBy == null || completionTime == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private SettlementCompletionResult failure(UUID settlementId, ErrorCode errorCode) {
        // 실패 사유는 응답에 담기지 않으므로 이 지점에서 정산 ID와 오류 코드를 남긴다.
        log.warn("정산 완료 실패 settlementId={}, code={}", settlementId, errorCode.name());
        return SettlementCompletionResult.failure(settlementId, errorCode);
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
