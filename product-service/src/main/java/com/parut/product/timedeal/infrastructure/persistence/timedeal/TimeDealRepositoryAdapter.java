package com.parut.product.timedeal.infrastructure.persistence.timedeal;

import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimeDealRepositoryAdapter implements TimeDealRepository {

    private final JpaTimeDealRepository jpaTimeDealRepository;

    @Override
    public Optional<TimeDeal> findById(UUID timeDealId) {
        return jpaTimeDealRepository.findByIdAndDeletedAtIsNull(timeDealId);
    }

    @Override
    public Optional<TimeDeal> findByIdForUpdate(UUID timeDealId) {
        return jpaTimeDealRepository.findByIdForUpdate(timeDealId);
    }

    @Override
    public List<UUID> findTimeDealsToActivate(Instant now, UUID afterId, int limit) {
        return jpaTimeDealRepository.findTimeDealsToActivate(now, afterId, limit);
    }

    @Override
    public List<UUID> findTimeDealsToEnd(Instant now, UUID afterId, int limit) {
        return jpaTimeDealRepository.findTimeDealsToEnd(now, afterId, limit);
    }

    @Override
    public TimeDeal save(TimeDeal timeDeal) {
        return jpaTimeDealRepository.save(timeDeal);
    }

    @Override
    public TimeDeal saveAndFlush(TimeDeal timeDeal) {
        return jpaTimeDealRepository.saveAndFlush(timeDeal);
    }
}
