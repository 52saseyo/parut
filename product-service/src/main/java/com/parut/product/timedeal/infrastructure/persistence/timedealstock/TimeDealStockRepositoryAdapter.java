package com.parut.product.timedeal.infrastructure.persistence.timedealstock;

import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRepository;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TimeDealStockRepositoryAdapter implements TimeDealStockRepository {

    private final JpaTimeDealStockRepository jpaTimeDealStockRepository;

    @Override
    public Optional<TimeDealStock> findByTimeDealId(UUID timeDealId) {
        return jpaTimeDealStockRepository.findByTimeDealIdAndDeletedAtIsNull(timeDealId);
    }

    @Override
    public TimeDealStock save(TimeDealStock timeDealStock) {
        return jpaTimeDealStockRepository.save(timeDealStock);
    }
}