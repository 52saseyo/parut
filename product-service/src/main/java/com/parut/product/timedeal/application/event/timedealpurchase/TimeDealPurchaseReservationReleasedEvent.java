package com.parut.product.timedeal.application.event.timedealpurchase;

import java.util.UUID;

/**
 * RESERVED 구매가 취소되어 Redis 선점 재고를 반환해야 한다는 이벤트.
 * DB 상태 변경이 커밋된 뒤에만 Redis 보상을 실행한다.
 */
public record TimeDealPurchaseReservationReleasedEvent(
        UUID timeDealId,
        UUID stockId,
        UUID orderId
) {
}
