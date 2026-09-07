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

    /**
     * 1인당 누적 구매 수량에 합산할 상태.
     * CANCELLED를 제외하는 것이 핵심이다 — 취소한 수량은 다시 구매할 수 있어야 하므로
     * 누적에 포함하면 사용자가 취소 후 재구매를 못 하게 된다.
     * 이 집합을 넓히면 1인당 제한이 조용히 강해지므로 바꿀 때 주의한다.
     */
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
        // 수량 합이 int를 넘는 것은 데이터 이상이므로 잘라내지 않고 예외로 드러낸다.
        return Math.toIntExact(sum);
    }

    @Override
    public TimeDealPurchase save(TimeDealPurchase timeDealPurchase) {
        return jpaTimeDealPurchaseRepository.save(timeDealPurchase);
    }
}