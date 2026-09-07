package com.parut.product.timedeal.application.port.out.timedealpurchase;

import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchase;

import java.util.Optional;
import java.util.UUID;


public interface TimeDealPurchaseRepository {


    Optional<TimeDealPurchase> findByOrderId(UUID orderId);


    boolean existsByOrderId(UUID orderId);

    /**
     * 해당 사용자가 이 타임딜에서 현재 확보하고 있는 수량의 합.
     * TimeDeal.validatePurchaseQuantity(quantity, alreadyPurchasedQuantity)에 넘길 값이다.
     * 합산 대상은 RESERVED와 CONFIRMED뿐이고 CANCELLED는 제외한다 —
     * 취소한 수량은 다시 구매할 수 있어야 하므로 누적에 포함하면 안 된다.
     * 구매 이력이 없으면 0을 반환한다.
     */
    int sumActiveQuantity(UUID timeDealId, UUID userId);

    TimeDealPurchase save(TimeDealPurchase timeDealPurchase);
}