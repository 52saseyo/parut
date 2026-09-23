package com.parut.product.global.outbox.application.port.out;

import com.parut.product.global.outbox.domain.OutboxEvent;

public interface OutboxMessagePublisher {

    void publish(OutboxEvent event);
}
