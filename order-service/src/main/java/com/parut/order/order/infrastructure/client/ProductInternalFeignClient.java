package com.parut.order.order.infrastructure.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.parut.order.global.common.ApiResponse;
import com.parut.order.global.config.InternalFeignConfig;
import com.parut.order.order.infrastructure.client.dto.ProductOrderInfoApiResponse;
import com.parut.order.order.infrastructure.client.dto.ProductStockReserveApiRequest;
import com.parut.order.order.infrastructure.client.dto.ProductStockRestoreApiRequest;

@FeignClient(
        name = "product-service",
        url = "${product-service.base-url}",
        configuration = InternalFeignConfig.class
)
public interface ProductInternalFeignClient {

    @GetMapping("/api/v1/internal/products/{productId}/order-info")
    ApiResponse<ProductOrderInfoApiResponse> getOrderInfo(@PathVariable("productId") UUID productId);

    @PostMapping("/api/v1/internal/stocks/{productId}/reserve")
    ApiResponse<Void> reserveStock(
            @PathVariable("productId") UUID productId,
            @RequestBody ProductStockReserveApiRequest request
    );

    @PostMapping("/api/v1/internal/stocks/{productId}/restore")
    ApiResponse<Void> restoreStock(
            @PathVariable("productId") UUID productId,
            @RequestBody ProductStockRestoreApiRequest request
    );
}
