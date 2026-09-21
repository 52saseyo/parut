package com.parut.product.timedeal.application.port.out.timedealpurchase;

import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchase;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface TimeDealPurchaseRepository {


    Optional<TimeDealPurchase> findByOrderId(UUID orderId);

    Optional<TimeDealPurchase> findByOrderIdForUpdate(UUID orderId);

    Optional<TimeDealPurchase> findByIdForUpdate(UUID purchaseId);

    List<TimeDealPurchase> findFirstExpiredReservationBatch(Instant cutoff, int limit);

    List<TimeDealPurchase> findNextExpiredReservationBatchByCursor(
            Instant cutoff,
            Instant cursorExpiresAt,
            UUID cursorId,
            int limit
    );


    // NOTE: 해당 사용자가 이 타임딜에서 확보한 수량의 합(RESERVED + CONFIRMED, CANCELLED 제외).
    // 이력이 없으면 0을 반환한다.
    int sumActiveQuantity(UUID timeDealId, UUID userId);

    TimeDealPurchase save(TimeDealPurchase timeDealPurchase);

    TimeDealPurchase saveAndFlush(TimeDealPurchase timeDealPurchase);
}
