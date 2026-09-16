package com.parut.product.timedeal.application.port.in.timedealstock;

import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockQueryResult;

import java.util.UUID;

public interface TimeDealStockQueryUseCase {

    TimeDealStockQueryResult getStock(UUID timeDealId, UUID requesterId, String requesterRole);
}
