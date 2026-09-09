package com.parut.order.delivery.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parut.order.delivery.domain.Delivery;
import com.parut.order.delivery.domain.DeliveryStatus;
import com.parut.order.delivery.infrastructure.persistence.DeliveryRepository;
import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.in.OrderDeliveryGroupStatusUseCase;
import com.parut.order.order.application.port.in.OrderDeliveryGroupQueryUseCase;
import com.parut.order.order.application.port.in.dto.OrderDeliveryGroupView;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    private static final UUID DELIVERY_GROUP_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b8");
    private static final UUID SECOND_DELIVERY_GROUP_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b9");
    private static final UUID ORDER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6ba");
    private static final UUID SELLER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6bb");
    private static final Instant COMPLETION_TIME = Instant.parse("2026-09-05T07:00:00Z");
    private static final Instant COMPLETION_THRESHOLD = Instant.parse("2026-09-05T06:59:00Z");

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private OrderDeliveryGroupQueryUseCase orderDeliveryGroupQueryUseCase;

    @Mock
    private OrderDeliveryGroupStatusUseCase orderDeliveryGroupStatusUseCase;

    @InjectMocks
    private DeliveryService deliveryService;

    @Test
    @DisplayName("주문의 배송 그룹별로 PREPARING 배송을 한 건씩 생성한다")
    void 주문_배송_생성() {
        when(orderDeliveryGroupQueryUseCase.getDeliveryGroups(ORDER_ID)).thenReturn(List.of(
                new OrderDeliveryGroupView(DELIVERY_GROUP_ID, SELLER_ID, 1),
                new OrderDeliveryGroupView(SECOND_DELIVERY_GROUP_ID, SELLER_ID, 1)
        ));
        when(deliveryRepository.findByDeliveryGroupId(DELIVERY_GROUP_ID)).thenReturn(Optional.empty());
        when(deliveryRepository.findByDeliveryGroupId(SECOND_DELIVERY_GROUP_ID)).thenReturn(Optional.empty());
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        deliveryService.createDeliveries(ORDER_ID);

        ArgumentCaptor<Delivery> deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryRepository, times(2)).save(deliveryCaptor.capture());
        assertThat(deliveryCaptor.getAllValues())
                .extracting(Delivery::getDeliveryGroupId)
                .containsExactlyInAnyOrder(DELIVERY_GROUP_ID, SECOND_DELIVERY_GROUP_ID);
        assertThat(deliveryCaptor.getAllValues())
                .extracting(Delivery::getStatus)
                .containsOnly(DeliveryStatus.PREPARING);
    }

    @Test
    @DisplayName("배송 그룹에 기존 배송이 있으면 중복 생성하지 않는다")
    void 기존_배송_재사용() {
        Delivery existingDelivery = Delivery.create(DELIVERY_GROUP_ID);
        when(orderDeliveryGroupQueryUseCase.getDeliveryGroups(ORDER_ID)).thenReturn(List.of(
                new OrderDeliveryGroupView(DELIVERY_GROUP_ID, SELLER_ID, 1)
        ));
        when(deliveryRepository.findByDeliveryGroupId(DELIVERY_GROUP_ID))
                .thenReturn(Optional.of(existingDelivery));

        deliveryService.createDeliveries(ORDER_ID);

        verify(deliveryRepository, never()).save(any(Delivery.class));
    }

    @Test
    @DisplayName("주문 ID 없이 배송을 생성할 수 없다")
    void 배송_생성_주문ID_누락() {
        assertThatThrownBy(() -> deliveryService.createDeliveries(null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("배송 시작 60초가 지난 배송을 완료 대상으로 조회한다")
    void 배송_완료_기준_시각_계산() {
        when(deliveryRepository.findAllByStatusAndShippedAtLessThanEqual(
                DeliveryStatus.SHIPPED,
                COMPLETION_THRESHOLD
        )).thenReturn(List.of());

        deliveryService.completeEligibleDeliveries(COMPLETION_TIME);

        verify(deliveryRepository).findAllByStatusAndShippedAtLessThanEqual(
                DeliveryStatus.SHIPPED,
                COMPLETION_THRESHOLD
        );
    }

    @Test
    @DisplayName("완료 대상 배송의 상태를 변경하고 Order 배송 그룹 상태를 동기화한다")
    void 배송_자동_완료() {
        Delivery delivery = Delivery.create(DELIVERY_GROUP_ID);
        delivery.ship("1234567890", COMPLETION_THRESHOLD);
        when(deliveryRepository.findAllByStatusAndShippedAtLessThanEqual(
                DeliveryStatus.SHIPPED,
                COMPLETION_THRESHOLD
        )).thenReturn(List.of(delivery));

        deliveryService.completeEligibleDeliveries(COMPLETION_TIME);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(delivery.getDeliveredAt()).isEqualTo(COMPLETION_TIME);
        verify(orderDeliveryGroupStatusUseCase).markDelivered(DELIVERY_GROUP_ID);
    }
}
