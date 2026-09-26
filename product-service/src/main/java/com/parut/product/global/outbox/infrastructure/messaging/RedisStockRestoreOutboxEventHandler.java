package com.parut.product.global.outbox.infrastructure.messaging;

import com.parut.product.global.outbox.application.port.out.OutboxEventHandler;
import com.parut.product.global.outbox.domain.OutboxEvent;
import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealStockRestorePayload;
import com.parut.product.timedeal.application.event.timedealpurchase.TimeDealStockRestoreRequestedEvent;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockReservationPort;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRestoreResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RedisStockRestoreOutboxEventHandler implements OutboxEventHandler {

    private final ObjectMapper objectMapper;
    private final TimeDealStockReservationPort timeDealStockReservationPort;

    @Override
    public boolean supports(String eventType) {
        return TimeDealStockRestoreRequestedEvent.EVENT_TYPE.equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) {
        try {
            TimeDealStockRestorePayload payload = objectMapper.readValue(
                    event.getPayload(), TimeDealStockRestorePayload.class);
            TimeDealStockRestoreResult result = timeDealStockReservationPort.restore(
                    payload.timeDealId(),
                    payload.stockId(),
                    payload.orderId(),
                    event.getEventId(),
                    payload.quantity());
            if (result == TimeDealStockRestoreResult.STOCK_NOT_INITIALIZED) {
                throw new IllegalStateException("Redis stock key is not initialized during restore");
            }
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Redis stock restore failed. eventId=" + event.getEventId(), exception);
        }
    }
}
