package com.parut.product.timedeal.application.port.out.timedeal;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealDetailView;

import java.util.Optional;
import java.util.UUID;

// NOTE: 변경용 TimeDealRepository와 분리한다 — 조회는 엔티티가 아니라 DTO를 직접 채운다.
public interface TimeDealQueryRepository {

    Optional<TimeDealDetailView> findDetailById(UUID timeDealId);
}
