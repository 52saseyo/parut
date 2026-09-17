package com.parut.order.delivery.application.port.in;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * 배송 그룹의 실제 배송 완료 시각을 제공한다.
 */
public interface DeliveryCompletionQueryUseCase {

    /** 배송 기록이 없거나 완료 전이면 빈 값을 반환한다. */
    Optional<Instant> getDeliveredAt(UUID deliveryGroupId);
}
