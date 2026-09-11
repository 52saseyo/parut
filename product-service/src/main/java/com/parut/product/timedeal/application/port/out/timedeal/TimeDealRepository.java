package com.parut.product.timedeal.application.port.out.timedeal;

import com.parut.product.timedeal.domain.timedeal.TimeDeal;

import java.util.Optional;
import java.util.UUID;

public interface TimeDealRepository {

    Optional<TimeDeal> findById(UUID timeDealId);

    TimeDeal save(TimeDeal timeDeal);

    // NOTE: 유니크 제약 위반을 커밋까지 미루지 않고 이 자리에서 드러내야 하는 경우에 쓴다
    // (전환 중복을 500이 아니라 409로 응답하기 위함).
    TimeDeal saveAndFlush(TimeDeal timeDeal);
}