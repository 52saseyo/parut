package com.parut.product.timedeal.application.event.timedealpurchase;

import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockCompensationResult;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockReservationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeDealPurchaseReservationReleaseListener {

    private final TimeDealStockReservationPort timeDealStockReservationPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void release(TimeDealPurchaseReservationReleasedEvent event) {
        try {
            TimeDealStockCompensationResult result = timeDealStockReservationPort.compensate(
                    event.timeDealId(),
                    event.stockId(),
                    event.orderId()
            );

            if (result == TimeDealStockCompensationResult.STOCK_NOT_INITIALIZED) {
                throw new IllegalStateException("Redis stock key is not initialized during reservation release");
            }

            log.info("[TimeDealPurchase] RESERVED 취소 후 Redis 선점 재고 보상 완료. timeDealId={}, stockId={}, orderId={}, result={}",
                    event.timeDealId(), event.stockId(), event.orderId(), result);
        } catch (RuntimeException exception) {
            // AFTER_COMMIT에서는 DB 롤백이 불가능하므로 재시도/Outbox로 확장할 수 있도록 오류를 남긴다.
            log.error("[TimeDealPurchase] RESERVED 취소 후 Redis 선점 재고 보상 실패. 재처리가 필요하다. timeDealId={}, stockId={}, orderId={}",
                    event.timeDealId(), event.stockId(), event.orderId(), exception);
        }
    }
}
