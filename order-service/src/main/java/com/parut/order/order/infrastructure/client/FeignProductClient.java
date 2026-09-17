package com.parut.order.order.infrastructure.client;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.out.ProductClient;
import com.parut.order.order.application.port.out.dto.ProductOrderInfo;
import com.parut.order.order.application.port.out.dto.ProductStockItem;
import com.parut.order.order.application.port.out.dto.ProductStockReserveItem;
import com.parut.order.order.infrastructure.client.dto.ProductOrderInfoApiRequest;
import com.parut.order.order.infrastructure.client.dto.ProductOrderInfoApiResponse;
import com.parut.order.order.infrastructure.client.dto.ProductStockReserveApiRequest;
import com.parut.order.order.infrastructure.client.dto.ProductStockRestoreApiRequest;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeignProductClient implements ProductClient {

    private final ProductInternalFeignClient productInternalFeignClient;

    @Override
    public List<ProductOrderInfo> getOrderInfos(List<UUID> productIds) {
        try {
            List<ProductOrderInfoApiResponse> responses = productInternalFeignClient
                    .getOrderInfos(new ProductOrderInfoApiRequest(productIds))
                    .data();
            return responses.stream()
                    .map(response -> new ProductOrderInfo(
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
                    ))
                    .toList();
        } catch (FeignException.NotFound e) {
            throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE);
        } catch (FeignException e) {
            log.warn("[ProductClient] 상품 조회 실패 productIds={}, status={}", productIds, e.status(), e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public void reserveStock(UUID orderId, List<ProductStockReserveItem> items) {
        try {
            productInternalFeignClient.reserveStock(new ProductStockReserveApiRequest(
                    orderId,
                    items.stream()
                            .map(item -> new ProductStockReserveApiRequest.Item(item.productId(), item.orderItemId(), item.quantity()))
                            .toList()
            ));
        } catch (FeignException.NotFound | FeignException.Conflict e) {
            throw new BusinessException(ErrorCode.STOCK_SHORTAGE);
        } catch (FeignException e) {
            log.warn("[ProductClient] 재고 예약 실패 orderId={}, items={}, status={}", orderId, items, e.status(), e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public void restoreStock(UUID orderId, List<ProductStockItem> items) {
        try {
            productInternalFeignClient.restoreStock(new ProductStockRestoreApiRequest(
                    orderId,
                    items.stream()
                            .map(item -> new ProductStockRestoreApiRequest.Item(item.productId(), item.orderItemId()))
                            .toList()
            ));
        } catch (FeignException e) {
            log.warn("[ProductClient] 재고 해제 실패 orderId={}, items={}, status={}", orderId, items, e.status(), e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
