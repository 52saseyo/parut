package com.parut.product.timedeal.infrastructure.persistence.timedeal;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealDetailView;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailView;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealQueryRepository;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

@Component
@RequiredArgsConstructor
public class TimeDealQueryRepositoryAdapter implements TimeDealQueryRepository {

    private final JpaTimeDealQueryRepository jpaTimeDealQueryRepository;

    @Override
    public List<TimeDealPublicDetailView> findPublicList(
            TimeDealStatus status, String cursor, UUID cursorId, int size) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        if (cursor == null) {
            return jpaTimeDealQueryRepository.findFirstPublicList(status, pageRequest);
        }
        return jpaTimeDealQueryRepository.findNextPublicList(
                status,
                java.time.Instant.parse(cursor),
                cursorId,
                pageRequest);
    }

    @Override
    public List<TimeDealPublicDetailView> findSellerOwnedTimeDealList(
            UUID sellerId, String cursor, UUID cursorId, int size) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        if (cursor == null) {
            return jpaTimeDealQueryRepository.findFirstSellerList(sellerId, pageRequest);
        }
        return jpaTimeDealQueryRepository.findNextSellerList(
                sellerId,
                java.time.Instant.parse(cursor),
                cursorId,
                pageRequest);
    }

    @Override
    public Optional<TimeDealPublicDetailView> findPublicDetailById(UUID timeDealId) {
        return jpaTimeDealQueryRepository.findPublicDetailById(timeDealId);
    }

    @Override
    public Optional<TimeDealDetailView> findDetailById(UUID timeDealId) {
        return jpaTimeDealQueryRepository.findDetailById(timeDealId);
    }

    @Override
    public List<TimeDealDetailView> findDetailsByIds(List<UUID> timeDealIds) {
        return jpaTimeDealQueryRepository.findDetailsByIds(timeDealIds);
    }
}
