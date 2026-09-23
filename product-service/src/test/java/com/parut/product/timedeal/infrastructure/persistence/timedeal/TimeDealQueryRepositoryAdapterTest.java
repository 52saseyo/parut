package com.parut.product.timedeal.infrastructure.persistence.timedeal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealOpeningSoonTarget;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealQueryRepository;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TimeDealQueryRepositoryAdapterTest {

    private final JpaTimeDealQueryRepository jpaRepository = org.mockito.Mockito.mock(JpaTimeDealQueryRepository.class);
    private final TimeDealQueryRepository adapter = new TimeDealQueryRepositoryAdapter(jpaRepository);

    @Test
    void 시작_임박_조회는_SCHEDULED_상태로_위임한다() {
        Instant now = Instant.parse("2026-09-22T10:00:00Z");
        Instant deadline = Instant.parse("2026-09-22T10:10:00Z");
        TimeDealOpeningSoonTarget target = new TimeDealOpeningSoonTarget(
                UUID.randomUUID(),
                "딸기 타임딜",
                deadline
        );
        when(jpaRepository.findOpeningSoonTargets(TimeDealStatus.SCHEDULED, now, deadline))
                .thenReturn(List.of(target));

        List<TimeDealOpeningSoonTarget> result = adapter.findOpeningSoonTargets(now, deadline);

        assertThat(result).containsExactly(target);
        verify(jpaRepository).findOpeningSoonTargets(TimeDealStatus.SCHEDULED, now, deadline);
    }
}
