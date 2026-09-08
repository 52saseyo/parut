package com.parut.product.timedeal.application.port.in.timedealpurchase;

import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseCancelCommand;
import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseConfirmCommand;
import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseReserveCommand;

public interface TimeDealPurchaseCommandUseCase {

    void reserve(TimeDealPurchaseReserveCommand timeDealPurchaseReserveCommand);

    // NOTE: 선점 만료로 확정하지 못하면 TIME_DEAL_RESERVATION_EXPIRED를 던진다.
    void confirm(TimeDealPurchaseConfirmCommand timeDealPurchaseConfirmCommand);

    void cancel(TimeDealPurchaseCancelCommand timeDealPurchaseCancelCommand);
}
