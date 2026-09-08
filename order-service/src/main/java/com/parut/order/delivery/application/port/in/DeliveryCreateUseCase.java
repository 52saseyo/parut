package com.parut.order.delivery.application.port.in;

import java.util.UUID;

/**
 * 결제 승인된 주문의 배송 그룹별 Delivery 생성을 요청한다.
 *
 * <p>Order와 배송 그룹 상태 변경은 호출자가 책임진다.
 */
public interface DeliveryCreateUseCase {

    void createDeliveries(UUID orderId);
}
