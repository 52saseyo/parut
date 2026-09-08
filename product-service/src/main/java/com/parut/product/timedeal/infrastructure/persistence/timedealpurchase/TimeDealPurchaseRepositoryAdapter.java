package com.parut.product.timedeal.infrastructure.persistence.timedealpurchase;

import com.parut.product.timedeal.application.port.out.timedealpurchase.TimeDealPurchaseRepository;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchase;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchaseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


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
    public boolean existsByOrderId(UUID orderId) {
        return jpaTimeDealPurchaseRepository.existsByOrderId(orderId);
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
}