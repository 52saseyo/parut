package com.parut.product.timedeal.infrastructure.persistence.timedealstock;

import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockListView;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TimeDealStockQueryRepositoryAdapter implements TimeDealStockQueryRepository {

    private final JpaTimeDealStockQueryRepository jpaTimeDealStockQueryRepository;

    @Override
    public List<TimeDealStockListView> findSellerOwnedTimeDealStockList(
            UUID sellerId, String cursor, UUID cursorId, int size) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        if (cursor == null) {
            return jpaTimeDealStockQueryRepository.findFirstSellerOwnedTimeDealStockList(
                    sellerId, pageRequest);
        }
        return jpaTimeDealStockQueryRepository.findNextSellerOwnedTimeDealStockList(
                sellerId, Instant.parse(cursor), cursorId, pageRequest);
    }
}
