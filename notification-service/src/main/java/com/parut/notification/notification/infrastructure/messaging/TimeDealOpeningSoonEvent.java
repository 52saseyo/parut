package com.parut.notification.notification.infrastructure.messaging;


import com.parut.notification.global.exception.InvalidKafkaEventException;
import com.parut.notification.notification.application.dto.TimeDealOpeningSoonCommand;

import java.time.Instant;
import java.util.UUID;

public record TimeDealOpeningSoonEvent(
        UUID eventId,
        UUID timeDealId,
        String timeDealName,
        Instant timeDealStartAt
) {
    public void validate() {
        if (eventId == null) {
            throw new InvalidKafkaEventException("이벤트 ID는 필수입니다.");
        }

        if (timeDealId == null) {
            throw new InvalidKafkaEventException("타임딜 ID는 필수입니다.");
        }

        if (timeDealName == null || timeDealName.isBlank()) {
            throw new InvalidKafkaEventException("타임딜 상품명은 필수입니다.");
        }

        if (timeDealStartAt == null) {
            throw new InvalidKafkaEventException("타임딜 시작 시각은 필수입니다.");
        }
    }
    public TimeDealOpeningSoonCommand toCommand(String traceId) {
        return new TimeDealOpeningSoonCommand(eventId, traceId, timeDealId, timeDealName, timeDealStartAt);
    }
}
