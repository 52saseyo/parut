package com.parut.product.timedeal.infrastructure.persistence.timedeal;

import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TimeDealRepositoryAdapter implements TimeDealRepository {

    private final JpaTimeDealRepository jpaTimeDealRepository;

    @Override
    public Optional<TimeDeal> findById(UUID timeDealId) {
        return jpaTimeDealRepository.findByIdAndDeletedAtIsNull(timeDealId);
    }

    @Override
    public TimeDeal save(TimeDeal timeDeal) {
        return jpaTimeDealRepository.save(timeDeal);
    }
}