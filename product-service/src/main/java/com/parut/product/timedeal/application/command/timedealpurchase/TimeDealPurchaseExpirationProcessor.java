package com.parut.product.timedeal.application.command.timedealpurchase;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.application.event.timedealpurchase.TimeDealPurchaseReservationReleasedEvent;
import com.parut.product.timedeal.application.port.out.timedealpurchase.TimeDealPurchaseRepository;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRepository;
import com.parut.product.timedeal.domain.common.TimeDealPolicy;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchase;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchaseStatus;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimeDealPurchaseExpirationProcessor {

    private final TimeDealPurchaseRepository timeDealPurchaseRepository;
    private final TimeDealStockRepository timeDealStockRepository;
    private final TimeDealPolicy timeDealPolicy;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireOneReservation(UUID purchaseId) { // NOTE: 각각의 트랜잭션을 독립적으로 가져가 하나가 실패해도 전체 롤백 되지않도록한다.
        // NOTE: 동시성 정합성을위해 비관락 사용
        TimeDealPurchase timeDealPurchase = timeDealPurchaseRepository.findByIdForUpdate(purchaseId)
                .orElse(null);

        if (timeDealPurchase == null || timeDealPurchase.getStatus() != TimeDealPurchaseStatus.RESERVED) {
            return;
        }

        // NOTE: 동시성 정합성을위해 비관락 사용, 이때 데드락이 걸리지 않도록 다른곳에서 purchase, stock 락 순서를 지킬것
        TimeDealStock timeDealStock = timeDealStockRepository
                .findByTimeDealIdForUpdate(timeDealPurchase.getTimeDealId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_STOCK_NOT_FOUND));

        Instant now = Instant.now();
        if (!timeDealPurchase.isExpired(now)) {
            return;
        }

        timeDealPolicy.expireReservation(timeDealPurchase, timeDealStock, now);

        // NOTE: 구입선점 만료 재고를 복구하기위해 해당 event를 발행
        eventPublisher.publishEvent(new TimeDealPurchaseReservationReleasedEvent(
                timeDealPurchase.getTimeDealId(),
                timeDealStock.getId(),
                timeDealPurchase.getOrderId()
        ));
    }
}
