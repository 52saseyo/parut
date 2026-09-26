package com.parut.product.global.outbox.application.port.out;

import com.parut.product.global.outbox.domain.OutboxEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent event);

    // NOTE: 동일한 멱등 키가 있으면 저장하지 않고 false를 반환한다.
    boolean saveIfAbsent(OutboxEvent event);

    List<OutboxEvent> findPending(int limit);

    Optional<OutboxEvent> findByEventId(UUID eventId);
}
