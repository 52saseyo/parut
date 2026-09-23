package com.parut.product.timedeal.application.command.timedeal;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.parut.product.global.logging.TraceIdContext;
import com.parut.product.global.outbox.application.port.out.OutboxEventRepository;
import com.parut.product.global.outbox.domain.OutboxEvent;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealOpeningSoonTarget;
import com.parut.product.timedeal.application.event.timedeal.TimeDealOpeningSoonEvent;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealQueryRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeDealOpeningSoonDetector {

    private static final Duration OPENING_SOON_NOTICE = Duration.ofMinutes(10);

    private final TimeDealQueryRepository timeDealQueryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void detect(Instant now) {
        String traceId = resolveTraceId();
        Instant deadline = now.plus(OPENING_SOON_NOTICE);
        List<TimeDealOpeningSoonTarget> targets =
                timeDealQueryRepository.findOpeningSoonTargets(now, deadline);

        int savedCount = 0;
        int skippedCount = 0;
        for (TimeDealOpeningSoonTarget target : targets) {
            TimeDealOpeningSoonEvent event = newEvent(target);
            OutboxEvent outboxEvent = toOutboxEvent(event, traceId, now);
            if (outboxEventRepository.saveIfAbsent(outboxEvent)) {
                savedCount++;
            } else {
                skippedCount++;
            }
        }

        log.info(
                "[TimeDealOpeningSoon] 임박 이벤트 감지 완료. targetCount={}, savedCount={}, skippedCount={}, now={}, deadline={}",
                targets.size(), savedCount, skippedCount, now, deadline);
    }

    private TimeDealOpeningSoonEvent newEvent(TimeDealOpeningSoonTarget target) {
        return new TimeDealOpeningSoonEvent(
                UUID.randomUUID(),
                target.timeDealId(),
                target.timeDealName(),
                target.timeDealStartAt()
        );
    }

    private OutboxEvent toOutboxEvent(
            TimeDealOpeningSoonEvent event,
            String traceId,
            Instant createdAt
    ) {
        try {
            return OutboxEvent.pending(
                    event.eventId(),
                    TimeDealOpeningSoonEvent.EVENT_TYPE,
                    event.timeDealId(),
                    event.timeDealStartAt().toString(),
                    traceId,
                    objectMapper.writeValueAsString(event),
                    createdAt
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "타임딜 시작 임박 이벤트 payload 생성에 실패했습니다. timeDealId=" + event.timeDealId(),
                    exception
            );
        }
    }

    private String resolveTraceId() {
        String currentTraceId = TraceIdContext.currentTraceId();
        if (currentTraceId != null && !currentTraceId.isBlank()) {
            return currentTraceId;
        }
        return UUID.randomUUID().toString();
    }
}
