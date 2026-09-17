package com.parut.product.timedeal.application.port.in.timedealstock;

import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockAdjustCommand;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockAdjustResult;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockTransferCommand;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockTransferResult;

public interface TimeDealStockCommandUseCase {

    TimeDealStockAdjustResult adjustStock(TimeDealStockAdjustCommand timeDealStockAdjustCommand);

    TimeDealStockTransferResult transferStock(TimeDealStockTransferCommand timeDealStockTransferCommand);
}
