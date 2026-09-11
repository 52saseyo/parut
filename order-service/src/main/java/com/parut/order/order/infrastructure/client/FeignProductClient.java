package com.parut.order.order.infrastructure.client;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.out.ProductClient;
import com.parut.order.order.application.port.out.dto.ProductOrderInfo;
import com.parut.order.order.infrastructure.client.dto.ProductOrderInfoApiResponse;
import com.parut.order.order.infrastructure.client.dto.ProductStockReserveApiRequest;
import com.parut.order.order.infrastructure.client.dto.ProductStockRestoreApiRequest;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeignProductClient implements ProductClient {

    private final ProductInternalFeignClient productInternalFeignClient;

    @Override
    public ProductOrderInfo getOrderInfo(UUID productId) {
        try {
            ProductOrderInfoApiResponse response = productInternalFeignClient.getOrderInfo(productId).data();
            return new ProductOrderInfo(
                    response.productId(),
                    response.sellerId(),
                    response.productName(),
                    response.appearanceType(),
                    response.origin(),
                    response.harvestDate(),
                    response.saleUnit(),
                    response.unitQuantity(),
                    response.originalPrice(),
                    response.purchasable()
            );
        } catch (FeignException.NotFound e) {
            throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE);
        } catch (FeignException e) {
            log.warn("[ProductClient] 상품 조회 실패 productId={}, status={}", productId, e.status(), e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public void reserveStock(UUID productId, UUID orderId, UUID orderItemId, int quantity) {
        try {
            productInternalFeignClient.reserveStock(productId, new ProductStockReserveApiRequest(orderId, orderItemId, quantity));
        } catch (FeignException.NotFound | FeignException.Conflict e) {
            throw new BusinessException(ErrorCode.STOCK_SHORTAGE);
        } catch (FeignException e) {
            log.warn("[ProductClient] 재고 예약 실패 productId={}, orderItemId={}, status={}",
                    productId, orderItemId, e.status(), e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public void restoreStock(UUID productId, UUID orderId, UUID orderItemId) {
        try {
            productInternalFeignClient.restoreStock(productId, new ProductStockRestoreApiRequest(orderId, orderItemId));
        } catch (FeignException e) {
            log.warn("[ProductClient] 재고 해제 실패 productId={}, orderItemId={}, status={}",
                    productId, orderItemId, e.status(), e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
