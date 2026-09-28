package com.parut.product.timedeal.infrastructure.persistence.timedeal;

import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealScheduleEntry;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
    public List<UUID> findTimeDealsAvailableForRedis(Instant now) {
        return jpaTimeDealRepository.findTimeDealsAvailableForRedis(now);
    }

    @Override
    public List<TimeDealScheduleEntry> findSalePeriodSchedulesAfter(UUID lastSeenId, int limit) {
        PageRequest page = PageRequest.of(0, limit);
        List<JpaTimeDealRepository.TimeDealScheduleProjection> schedules = lastSeenId == null
                ? jpaTimeDealRepository.findFirstSalePeriodSchedules(page)
                : jpaTimeDealRepository.findSalePeriodSchedulesAfter(lastSeenId, page);

        return schedules.stream()
                .map(schedule -> new TimeDealScheduleEntry(
                        schedule.getId(), schedule.getStartAt(), schedule.getEndAt()))
                .toList();
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
