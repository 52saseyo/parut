package com.parut.order.delivery.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import com.parut.order.delivery.application.event.DeliveryStatusChangedEvent;
import com.parut.order.delivery.application.port.OrderDeliveryGroupQueryPort;
import com.parut.order.delivery.domain.Delivery;
import com.parut.order.delivery.domain.DeliveryStatus;
import com.parut.order.delivery.infrastructure.persistence.DeliveryRepository;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    private static final UUID DELIVERY_GROUP_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b8");
    private static final Instant COMPLETION_TIME = Instant.parse("2026-09-05T07:00:00Z");
    private static final Instant COMPLETION_THRESHOLD = Instant.parse("2026-09-05T06:59:00Z");

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private ObjectProvider<OrderDeliveryGroupQueryPort> orderDeliveryGroupQueryPortProvider;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeliveryService deliveryService;

    @Test
    @DisplayName("배송 시작 60초가 지난 배송을 완료 대상으로 조회한다")
    void 배송_완료_기준_시각_계산() {
        when(deliveryRepository.findAllByStatusAndShippedAtLessThanEqual(
                DeliveryStatus.SHIPPED,
                COMPLETION_THRESHOLD
        )).thenReturn(List.of());

        int completedCount = deliveryService.completeEligibleDeliveries(COMPLETION_TIME);

        assertThat(completedCount).isZero();
        verify(deliveryRepository).findAllByStatusAndShippedAtLessThanEqual(
                DeliveryStatus.SHIPPED,
                COMPLETION_THRESHOLD
        );
    }

    @Test
    @DisplayName("완료 대상 배송의 상태를 변경하고 이벤트를 발행한다")
    void 배송_자동_완료() {
        Delivery delivery = Delivery.create(DELIVERY_GROUP_ID);
        delivery.ship("1234567890", COMPLETION_THRESHOLD);
        when(deliveryRepository.findAllByStatusAndShippedAtLessThanEqual(
                DeliveryStatus.SHIPPED,
                COMPLETION_THRESHOLD
        )).thenReturn(List.of(delivery));

        int completedCount = deliveryService.completeEligibleDeliveries(COMPLETION_TIME);

        assertThat(completedCount).isOne();
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(delivery.getDeliveredAt()).isEqualTo(COMPLETION_TIME);
        verify(eventPublisher).publishEvent(DeliveryStatusChangedEvent.delivered(DELIVERY_GROUP_ID));
    }
}
