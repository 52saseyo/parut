package com.parut.order.delivery.application;

import java.util.List;
import java.util.UUID;

import com.parut.order.delivery.domain.Delivery;

/** 배송 목록과 다음 조회에 사용할 생성 시각, ID 커서를 전달한다. */
public record DeliveryPage(
        List<Delivery> content,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext
) {
}
