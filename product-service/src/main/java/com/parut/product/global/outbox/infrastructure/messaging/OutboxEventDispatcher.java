package com.parut.product.global.outbox.infrastructure.messaging;

import com.parut.product.global.outbox.application.port.out.OutboxEventHandler;
import com.parut.product.global.outbox.application.port.out.OutboxMessagePublisher;
import com.parut.product.global.outbox.domain.OutboxEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventDispatcher implements OutboxMessagePublisher { // NOTE: dispatcher의 역할은 event type 을보고 kafka, redis 핸들러를 선택하여 해당 핸들러의 handle() 메서드 실행

    private final List<OutboxEventHandler> handlers; // NOTE: OutboxEventHandler를 구현한 수만큼 리스트에 저장

    @Override
    public void publish(OutboxEvent event) {
        OutboxEventHandler handler = handlers.stream()
                .filter(candidate -> candidate.supports(event.getEventType())) // NOTE: eventHandler 중에 event type으로 필터하여 해당 핸들러를 선택
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 Outbox eventType입니다. eventType=" + event.getEventType()));

        handler.handle(event); // NOTE: 선택된 handler의 handle 메서드를 실행
    }
}
