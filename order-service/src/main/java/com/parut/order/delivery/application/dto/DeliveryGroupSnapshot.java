package com.parut.order.delivery.application.dto;

import java.util.UUID;

/**
 * 배송 생성과 시작 검증에 필요한 배송 그룹 정보.
 */
public record DeliveryGroupSnapshot(
        UUID deliveryGroupId,
        UUID sellerId,
        int nonCanceledItemCount
) {
}
