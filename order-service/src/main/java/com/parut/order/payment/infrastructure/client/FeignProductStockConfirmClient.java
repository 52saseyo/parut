package com.parut.order.payment.infrastructure.client;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.payment.application.port.out.ProductStockConfirmClient;
import com.parut.order.payment.application.port.out.dto.ProductStockConfirmItem;
import com.parut.order.payment.infrastructure.client.dto.ProductStockConfirmApiRequest;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeignProductStockConfirmClient implements ProductStockConfirmClient {

    private final ProductStockConfirmFeignClient productStockConfirmFeignClient;

    @Override
    public void confirmStock(UUID orderId, List<ProductStockConfirmItem> items) {
        try {
            productStockConfirmFeignClient.confirmStock(new ProductStockConfirmApiRequest(
                    orderId,
                    items.stream()
                            .map(item -> new ProductStockConfirmApiRequest.Item(item.productId(), item.orderItemId()))
                            .toList()
            ));
        } catch (FeignException.NotFound | FeignException.Conflict e) {
            throw new BusinessException(ErrorCode.STOCK_SHORTAGE);
        } catch (FeignException e) {
            log.warn("[ProductStockConfirmClient] 재고 확정 실패 orderId={}, items={}, status={}", orderId, items, e.status(), e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
