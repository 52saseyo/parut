package com.parut.product.timedeal.application.event.timedealpurchase;

import java.util.UUID;

/**
 * 기존 Outbox에 저장된 Redis 재고 복구 이벤트의 즉시 처리를 요청하는 이벤트.
 * DB 상태 변경이 커밋된 뒤에만 Redis 보상을 실행한다.
 */
public record TimeDealPurchaseReservationReleasedEvent(
        UUID eventId
) {
}
