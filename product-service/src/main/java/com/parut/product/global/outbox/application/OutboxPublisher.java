package com.parut.product.global.outbox.application;

import com.parut.product.global.outbox.application.port.out.OutboxEventRepository;
import com.parut.product.global.outbox.domain.OutboxEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox Publisher의 애플리케이션 진입점이다.
 *
 * 현재 단계에서는 PENDING 이벤트를 배치로 조회하는 책임만 가진다.
 * Kafka 발행과 발행 상태 변경은 다음 단계에서 이 흐름에 연결한다.
 */
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional(readOnly = true)
    public List<OutboxEvent> loadPendingEvents(int batchSize) {
        return outboxEventRepository.findPending(batchSize);
    }
}
