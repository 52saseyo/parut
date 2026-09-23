package com.parut.product.timedeal.application.port.out.timedeal;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealDetailView;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealOpeningSoonTarget;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailView;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

// NOTE: 변경용 TimeDealRepository와 분리한다 — 조회는 엔티티가 아니라 DTO를 직접 채운다.
public interface TimeDealQueryRepository {

    List<TimeDealOpeningSoonTarget> findOpeningSoonTargets(Instant now, Instant deadline);

    List<TimeDealPublicDetailView> findPublicList(
            TimeDealStatus status,
            String cursor,
            UUID cursorId,
            int size
    );

    List<TimeDealPublicDetailView> findSellerOwnedTimeDealList(
            UUID sellerId,
            String cursor,
            UUID cursorId,
            int size
    );

    Optional<TimeDealPublicDetailView> findPublicDetailById(UUID timeDealId);

    Optional<TimeDealDetailView> findDetailById(UUID timeDealId);

    List<TimeDealDetailView> findDetailsByIds(List<UUID> timeDealIds);
}
