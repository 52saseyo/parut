package com.parut.order.delivery.application.event;

import java.util.UUID;

import com.parut.order.delivery.domain.DeliveryStatus;

// TODO: Order Listener를 연결해 배송 그룹 상태를 갱신한다.
/** Order의 배송 그룹 상태를 맞추기 위한 내부 이벤트. */
public record DeliveryStatusChangedEvent(
        UUID deliveryGroupId,
        DeliveryStatus status
) {

    public static DeliveryStatusChangedEvent shipped(UUID deliveryGroupId) {
        return new DeliveryStatusChangedEvent(deliveryGroupId, DeliveryStatus.SHIPPED);
    }

    public static DeliveryStatusChangedEvent delivered(UUID deliveryGroupId) {
        return new DeliveryStatusChangedEvent(deliveryGroupId, DeliveryStatus.DELIVERED);
    }
}
