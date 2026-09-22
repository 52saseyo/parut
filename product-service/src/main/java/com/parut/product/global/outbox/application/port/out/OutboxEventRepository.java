package com.parut.product.global.outbox.application.port.out;

import com.parut.product.global.outbox.domain.OutboxEvent;
import java.util.List;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent event);

    List<OutboxEvent> findPending(int limit);
}
