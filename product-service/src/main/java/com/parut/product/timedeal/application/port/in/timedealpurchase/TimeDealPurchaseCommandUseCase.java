package com.parut.product.timedeal.application.port.in.timedealpurchase;

import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseCancelCommand;
import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseConfirmCommand;
import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseReserveCommand;

public interface TimeDealPurchaseCommandUseCase {

    void reserve(TimeDealPurchaseReserveCommand command);

    // NOTE: 구현체는 정리를 별도 트랜잭션(REQUIRES_NEW)에서 수행한 뒤 예외를 던져야 한다.
    void confirm(TimeDealPurchaseConfirmCommand command);

    void cancel(TimeDealPurchaseCancelCommand command);
}