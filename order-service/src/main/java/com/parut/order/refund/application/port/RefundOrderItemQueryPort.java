package com.parut.order.refund.application.port;

import java.util.Optional;
import java.util.UUID;

import com.parut.order.refund.application.dto.RefundOrderItemSnapshot;

/**
 * Order가 Refund에 제공하는 주문상품 조회 계약.
 *
 * <p>Refund에서는 Order의 Repository를 직접 사용하지 않는다.
 */
public interface RefundOrderItemQueryPort {

    Optional<RefundOrderItemSnapshot> findById(UUID orderItemId);
}
