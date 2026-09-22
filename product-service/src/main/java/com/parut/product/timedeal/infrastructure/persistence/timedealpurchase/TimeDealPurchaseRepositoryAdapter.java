package com.parut.product.timedeal.infrastructure.persistence.timedealpurchase;

import com.parut.product.timedeal.application.port.out.timedealpurchase.TimeDealPurchaseRepository;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchase;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchaseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;


@Component
@RequiredArgsConstructor
public class TimeDealPurchaseRepositoryAdapter implements TimeDealPurchaseRepository {

    // NOTE: CANCELLED를 제외하는 것이 핵심이다 — 포함하면 취소 후 재구매가 막힌다.
    // 이 집합을 넓히면 1인당 제한이 조용히 강해진다.
    private static final Set<TimeDealPurchaseStatus> ACTIVE_STATUSES =
            EnumSet.of(TimeDealPurchaseStatus.RESERVED, TimeDealPurchaseStatus.CONFIRMED);

    private final JpaTimeDealPurchaseRepository jpaTimeDealPurchaseRepository;

    @Override
    public Optional<TimeDealPurchase> findByOrderId(UUID orderId) {
        return jpaTimeDealPurchaseRepository.findByOrderId(orderId);
    }

    @Override
    public Optional<TimeDealPurchase> findByOrderIdForUpdate(UUID orderId) {
        return jpaTimeDealPurchaseRepository.findByOrderIdForUpdate(orderId);
    }

    @Override
    public Optional<TimeDealPurchase> findByIdForUpdate(UUID purchaseId) {
        return jpaTimeDealPurchaseRepository.findByIdForUpdate(purchaseId);
    }

    @Override
    public List<TimeDealPurchase> findFirstExpiredReservationBatch(Instant cutoff, int limit) {
        return jpaTimeDealPurchaseRepository.findFirstExpiredReservationBatch(cutoff, PageRequest.of(0, limit));
    }

    @Override
    public List<TimeDealPurchase> findNextExpiredReservationBatchByCursor(
            Instant cutoff,
            Instant cursorExpiresAt,
            UUID cursorId,
            int limit
    ) {
        return jpaTimeDealPurchaseRepository.findNextExpiredReservationBatchByCursor(
                cutoff,
                cursorExpiresAt,
                cursorId,
                PageRequest.of(0, limit)
        );
    }

    @Override
    public int sumActiveQuantity(UUID timeDealId, UUID userId) {
        long sum = jpaTimeDealPurchaseRepository.sumQuantityByStatusIn(timeDealId, userId, ACTIVE_STATUSES);
        // NOTE: int를 넘는 합은 데이터 이상이므로 잘라내지 않고 예외로 드러낸다.
        return Math.toIntExact(sum);
    }

    @Override
    public TimeDealPurchase save(TimeDealPurchase timeDealPurchase) {
        return jpaTimeDealPurchaseRepository.save(timeDealPurchase);
    }

    @Override
    public TimeDealPurchase saveAndFlush(TimeDealPurchase timeDealPurchase) {
        return jpaTimeDealPurchaseRepository.saveAndFlush(timeDealPurchase);
    }
}
