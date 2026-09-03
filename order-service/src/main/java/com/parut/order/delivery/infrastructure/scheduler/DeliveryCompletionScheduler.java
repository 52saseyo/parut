package com.parut.order.delivery.infrastructure.scheduler;

import java.time.OffsetDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.parut.order.delivery.application.DeliveryService;

import lombok.RequiredArgsConstructor;

/**
 * 배송 자동완료 작업을 주기적으로 실행한다.
 *
 * <p>실행 간격은 {@code delivery.completion.fixed-delay}로 조정할 수 있다.
 */
@Component
@RequiredArgsConstructor
public class DeliveryCompletionScheduler {

    private final DeliveryService deliveryService;

    @Scheduled(fixedDelayString = "${delivery.completion.fixed-delay:60000}")
    public void runDeliveryCompletion() {
        deliveryService.completeEligibleDeliveries(OffsetDateTime.now());
    }
}
