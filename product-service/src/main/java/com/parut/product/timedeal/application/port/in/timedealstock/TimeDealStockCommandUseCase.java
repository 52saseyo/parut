package com.parut.product.timedeal.application.port.in.timedealstock;

import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockAdjustCommand;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockAdjustResult;

public interface TimeDealStockCommandUseCase {

    TimeDealStockAdjustResult adjustStock(TimeDealStockAdjustCommand timeDealStockAdjustCommand);

    Void transferStock();
}
