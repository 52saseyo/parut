package com.parut.order.payment.infrastructure.client;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.payment.application.port.out.ProductStockConfirmClient;
import com.parut.order.payment.infrastructure.client.dto.ProductStockConfirmApiRequest;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeignProductStockConfirmClient implements ProductStockConfirmClient {

    private final ProductStockConfirmFeignClient productStockConfirmFeignClient;

    @Override
    public void confirmStock(UUID productId, UUID orderId, UUID orderItemId) {
        try {
            productStockConfirmFeignClient.confirmStock(productId, new ProductStockConfirmApiRequest(orderId, orderItemId));
        } catch (FeignException.NotFound | FeignException.Conflict e) {
            // 예약이 없거나(만료 정리 포함) 이미 처리됨 -> 재고 확정 실패로 간주
            throw new BusinessException(ErrorCode.STOCK_SHORTAGE);
        } catch (FeignException e) {
            log.warn("[ProductStockConfirmClient] 재고 확정 실패 productId={}, orderItemId={}, status={}",
                    productId, orderItemId, e.status(), e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
