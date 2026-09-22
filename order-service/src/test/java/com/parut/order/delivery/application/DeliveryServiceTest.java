package com.parut.order.delivery.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
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
import org.springframework.data.domain.Pageable;

import com.parut.order.delivery.domain.Delivery;
import com.parut.order.delivery.domain.DeliveryStatus;
import com.parut.order.delivery.infrastructure.persistence.DeliveryRepository;
import com.parut.order.global.auth.UserRole;
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
    private static final UUID DELIVERY_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6bd");
    private static final UUID SELLER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6bb");
    private static final UUID CUSTOMER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6be");
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
                group(DELIVERY_GROUP_ID, SELLER_ID),
                group(SECOND_DELIVERY_GROUP_ID, SELLER_ID)
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
        Delivery existingDelivery = delivery(DELIVERY_GROUP_ID, SELLER_ID);
        when(orderDeliveryGroupQueryUseCase.getDeliveryGroups(ORDER_ID)).thenReturn(List.of(
                group(DELIVERY_GROUP_ID, SELLER_ID)
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
    @DisplayName("고객 배송 목록은 소유자 조건으로 조회한다")
    void 고객_배송_목록_조회() {
        Delivery first = mock(Delivery.class);
        List<Delivery> deliveries = List.of(first);
        when(deliveryRepository.findCustomerDeliveries(
                org.mockito.ArgumentMatchers.eq(CUSTOMER_ID),
                org.mockito.ArgumentMatchers.eq(ORDER_ID),
                org.mockito.ArgumentMatchers.eq(DeliveryStatus.PREPARING),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any(Pageable.class)
        )).thenReturn(deliveries);

        DeliveryPage result = deliveryService.getDeliveries(
                CUSTOMER_ID, UserRole.CUSTOMER, ORDER_ID, DeliveryStatus.PREPARING, null, null, 10);

        assertThat(result.content()).containsExactly(first);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("구매자와 판매자는 본인 배송을, 관리자는 모든 배송을 단건 조회한다")
    void 배송_단건_조회() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = delivery(DELIVERY_GROUP_ID, SELLER_ID);
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));

        assertThat(deliveryService.getDelivery(deliveryId, CUSTOMER_ID, UserRole.CUSTOMER)).isSameAs(delivery);
        assertThat(deliveryService.getDelivery(deliveryId, SELLER_ID, UserRole.SELLER)).isSameAs(delivery);
        assertThat(deliveryService.getDelivery(deliveryId, UUID.randomUUID(), UserRole.ADMIN)).isSameAs(delivery);
    }

    @Test
    @DisplayName("다른 사람의 배송이나 허용되지 않은 역할의 단건 조회를 거부한다")
    void 배송_단건_조회_권한_없음() {
        UUID deliveryId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        when(deliveryRepository.findById(deliveryId))
                .thenReturn(Optional.of(delivery(DELIVERY_GROUP_ID, SELLER_ID)));

        assertThatThrownBy(() -> deliveryService.getDelivery(deliveryId, otherUserId, UserRole.CUSTOMER))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        assertThatThrownBy(() -> deliveryService.getDelivery(deliveryId, otherUserId, UserRole.SELLER))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("없는 배송의 단건 조회는 DELIVERY_NOT_FOUND를 반환한다")
    void 배송_단건_조회_대상_없음() {
        assertThatThrownBy(() -> deliveryService.getDelivery(UUID.randomUUID(), SELLER_ID, UserRole.SELLER))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.DELIVERY_NOT_FOUND);
    }

    @Test
    @DisplayName("완료 대상 배송과 주문 배송 그룹을 함께 완료한다")
    void 배송_자동_완료() {
        Delivery delivery = delivery(DELIVERY_GROUP_ID, SELLER_ID);
        delivery.ship("1234567890", COMPLETION_THRESHOLD);
        when(deliveryRepository.findById(DELIVERY_ID)).thenReturn(Optional.of(delivery));

        deliveryService.completeEligibleDelivery(DELIVERY_ID, COMPLETION_TIME, COMPLETION_THRESHOLD);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(delivery.getDeliveredAt()).isEqualTo(COMPLETION_TIME);
        verify(orderDeliveryGroupStatusUseCase).markDelivered(DELIVERY_GROUP_ID);
    }

    @Test
    @DisplayName("완료 기준보다 늦게 시작한 배송은 건너뛴다")
    void 완료_대상_재확인() {
        Delivery delivery = delivery(DELIVERY_GROUP_ID, SELLER_ID);
        delivery.ship("1234567890", COMPLETION_THRESHOLD.plusSeconds(1));
        when(deliveryRepository.findById(DELIVERY_ID)).thenReturn(Optional.of(delivery));

        deliveryService.completeEligibleDelivery(DELIVERY_ID, COMPLETION_TIME, COMPLETION_THRESHOLD);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SHIPPED);
        verify(orderDeliveryGroupStatusUseCase, never()).markDelivered(DELIVERY_GROUP_ID);
    }

    private OrderDeliveryGroupView group(UUID deliveryGroupId, UUID sellerId) {
        return new OrderDeliveryGroupView(deliveryGroupId, ORDER_ID, CUSTOMER_ID, sellerId, 1);
    }

    private Delivery delivery(UUID deliveryGroupId, UUID sellerId) {
        return Delivery.create(deliveryGroupId, ORDER_ID, CUSTOMER_ID, sellerId);
    }
}
