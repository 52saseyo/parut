package com.parut.product.timedeal.application.port.in.timedealstock;

import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockQueryResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCursorResult;

import java.util.UUID;

public interface TimeDealStockQueryUseCase {

    TimeDealStockQueryResult getStock(UUID timeDealId, UUID requesterId, String requesterRole);

    TimeDealCursorResult<TimeDealStockQueryResult> getSellerOwnedTimeDealStockList(
            UUID sellerId,
            String requesterRole,
            String cursor,
            UUID cursorId,
            int size
    );
}
