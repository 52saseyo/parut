package com.parut.order.payment.infrastructure.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.parut.order.global.common.ApiResponse;
import com.parut.order.global.config.InternalFeignConfig;
import com.parut.order.payment.infrastructure.client.dto.ProductStockConfirmApiRequest;

@FeignClient(
        name = "product-service",
        contextId = "productStockConfirmFeignClient",
        url = "${product-service.base-url}",
        configuration = InternalFeignConfig.class
)
public interface ProductStockConfirmFeignClient {

    @PostMapping("/api/v1/internal/stocks/{productId}/confirm")
    ApiResponse<Void> confirmStock(
            @PathVariable("productId") UUID productId,
            @RequestBody ProductStockConfirmApiRequest request
    );
}
