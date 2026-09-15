package com.parut.product.timedeal.application.port.out.timedeal;

import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeDealRepository {

    // 일반 조회. 행 잠금을 사용하지 않는다.
    Optional<TimeDeal> findById(UUID timeDealId);

    // 타임딜 자체를 변경하는 유즈케이스 전용 조회. 트랜잭션 종료까지 행 잠금을 유지한다.
    Optional<TimeDeal> findByIdForUpdate(UUID timeDealId);

    List<UUID> findTimeDealsToActivate(Instant now, UUID afterId, int limit);

    List<UUID> findTimeDealsToEnd(Instant now, UUID afterId, int limit);

    TimeDeal save(TimeDeal timeDeal);

    // NOTE: 유니크 제약 위반을 커밋까지 미루지 않고 이 자리에서 드러내야 하는 경우에 쓴다
    // (전환 중복을 500이 아니라 409로 응답하기 위함).
    TimeDeal saveAndFlush(TimeDeal timeDeal);
}
