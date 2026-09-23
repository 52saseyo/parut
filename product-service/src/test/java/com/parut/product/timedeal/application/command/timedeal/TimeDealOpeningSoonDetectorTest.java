package com.parut.product.timedeal.application.command.timedeal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import tools.jackson.databind.ObjectMapper;
import com.parut.product.global.outbox.application.port.out.OutboxEventRepository;
import com.parut.product.global.outbox.domain.OutboxEvent;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealOpeningSoonTarget;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealQueryRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TimeDealOpeningSoonDetectorTest {

    private static final Instant NOW = Instant.parse("2026-09-23T10:00:00Z");
    private static final Instant START_AT = Instant.parse("2026-09-23T10:10:00Z");

    private final TimeDealQueryRepository timeDealQueryRepository =
            org.mockito.Mockito.mock(TimeDealQueryRepository.class);
    private final OutboxEventRepository outboxEventRepository =
            org.mockito.Mockito.mock(OutboxEventRepository.class);
    private final ObjectMapper objectMapper = org.mockito.Mockito.mock(ObjectMapper.class);
    private final TimeDealOpeningSoonDetector detector = new TimeDealOpeningSoonDetector(
            timeDealQueryRepository,
            outboxEventRepository,
            objectMapper
    );

    @BeforeEach
    void setUp() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventId\":\"event\"}");
        when(outboxEventRepository.saveIfAbsent(any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        com.parut.product.global.logging.TraceIdContext.clear();
    }

    @Test
    void 조회된_타임딜을_시작임박_이벤트_Outbox로_저장한다() throws Exception {
        UUID timeDealId = UUID.randomUUID();
        when(timeDealQueryRepository.findOpeningSoonTargets(NOW, START_AT))
                .thenReturn(List.of(new TimeDealOpeningSoonTarget(timeDealId, "딸기 타임딜", START_AT)));
        com.parut.product.global.logging.TraceIdContext.set("request-trace-id");

        detector.detect(NOW);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).saveIfAbsent(captor.capture());
        OutboxEvent saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("TIME_DEAL_OPENING_SOON");
        assertThat(saved.getAggregateId()).isEqualTo(timeDealId);
        assertThat(saved.getDeduplicationKey()).isEqualTo(START_AT.toString());
        assertThat(saved.getTraceId()).isEqualTo("request-trace-id");
        assertThat(saved.getPayload()).isEqualTo("{\"eventId\":\"event\"}");
    }

    @Test
    void 요청_traceId가_없으면_스케줄러용_traceId를_생성한다() {
        UUID timeDealId = UUID.randomUUID();
        when(timeDealQueryRepository.findOpeningSoonTargets(NOW, START_AT))
                .thenReturn(List.of(new TimeDealOpeningSoonTarget(timeDealId, "딸기 타임딜", START_AT)));

        detector.detect(NOW);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).saveIfAbsent(captor.capture());
        assertThat(captor.getValue().getTraceId()).isNotBlank();
    }

    @Test
    void 대상이_없으면_Outbox를_저장하지_않는다() {
        when(timeDealQueryRepository.findOpeningSoonTargets(NOW, START_AT)).thenReturn(List.of());

        detector.detect(NOW);

        verify(outboxEventRepository, never()).saveIfAbsent(any());
    }
}
