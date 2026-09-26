package com.parut.product.global.outbox.application.port.out;

import com.parut.product.global.outbox.domain.OutboxEvent;

public interface OutboxEventHandler {

    boolean supports(String eventType);

    void handle(OutboxEvent event);
}
