package com.parut.product.timedeal.application.port.out.timedealstock;

import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockListView;

import java.util.List;
import java.util.UUID;

public interface TimeDealStockQueryRepository {

    List<TimeDealStockListView> findSellerOwnedTimeDealStockList(
            UUID sellerId,
            String cursor,
            UUID cursorId,
            int size
    );
}
