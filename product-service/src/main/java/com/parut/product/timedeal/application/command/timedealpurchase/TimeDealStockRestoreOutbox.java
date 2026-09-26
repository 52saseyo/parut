package com.parut.product.timedeal.application.command.timedealpurchase;

import com.parut.product.global.outbox.application.port.out.OutboxEventRepository;
import com.parut.product.global.outbox.domain.OutboxEvent;
import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealStockRestorePayload;
import com.parut.product.timedeal.application.event.timedealpurchase.TimeDealPurchaseReservationReleasedEvent;
import com.parut.product.timedeal.application.event.timedealpurchase.TimeDealStockRestoreRequestedEvent;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchase;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimeDealStockRestoreOutbox {
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    // 반드시 구매 취소/만료와 같은 트랜잭션에 저장한다.
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(TimeDealPurchase purchase, TimeDealStock stock) {
        UUID eventId = UUID.randomUUID();
        TimeDealStockRestorePayload payload = new TimeDealStockRestorePayload(
                purchase.getTimeDealId(), stock.getId(), purchase.getOrderId(), purchase.getQuantity());
        try {
            OutboxEvent event = OutboxEvent.pending(
                    eventId,
                    TimeDealStockRestoreRequestedEvent.EVENT_TYPE,
                    purchase.getOrderId(),
                    purchase.getOrderId().toString(),
                    UUID.randomUUID().toString(),
                    objectMapper.writeValueAsString(payload),
                    Instant.now());
            repository.saveIfAbsent(event);
            eventPublisher.publishEvent(new TimeDealPurchaseReservationReleasedEvent(eventId));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Redis 재고 복구 Outbox payload 생성에 실패했습니다.", exception);
        }
    }
}
