package com.parut.product.timedeal.application.port.in.timedealpurchase;

import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseCancelCommand;
import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseConfirmCommand;
import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseReserveCommand;

public interface TimeDealPurchaseCommandUseCase {

    void reserve(TimeDealPurchaseReserveCommand timeDealPurchaseReserveCommand);

    // NOTE: 선점 만료로 확정하지 못하면 TIME_DEAL_RESERVATION_EXPIRED를 던진다.
    void confirm(TimeDealPurchaseConfirmCommand timeDealPurchaseConfirmCommand);

    // NOTE: 배송 시작 전 주문 취소 전용이다 — 타임딜 재고를 복구하므로, 재고를 복구하지 않는
    // 환불(배송 완료 후) 흐름에서는 호출하지 않는다. 배송 상태를 모르는 이 서비스는 그것을 검증할 수 없다.
    void cancel(TimeDealPurchaseCancelCommand timeDealPurchaseCancelCommand);
}
