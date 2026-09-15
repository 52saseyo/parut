package com.parut.product.timedeal.infrastructure.persistence.timedeal;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealDetailView;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailView;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TimeDealQueryRepositoryAdapter implements TimeDealQueryRepository {

    private final JpaTimeDealQueryRepository jpaTimeDealQueryRepository;

    @Override
    public Optional<TimeDealPublicDetailView> findPublicDetailById(UUID timeDealId) {
        return jpaTimeDealQueryRepository.findPublicDetailById(timeDealId);
    }

    @Override
    public Optional<TimeDealDetailView> findDetailById(UUID timeDealId) {
        return jpaTimeDealQueryRepository.findDetailById(timeDealId);
    }
}
