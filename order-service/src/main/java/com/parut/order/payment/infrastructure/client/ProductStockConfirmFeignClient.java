package com.parut.order.payment.infrastructure.client;

import com.parut.order.global.common.ApiResponse;
import com.parut.order.global.config.InternalFeignConfig;
import com.parut.order.payment.infrastructure.client.dto.ProductStockConfirmApiRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "product-service",
        contextId = "productStockConfirmFeignClient",
        url = "${product-service.base-url}",
        configuration = InternalFeignConfig.class
)
public interface ProductStockConfirmFeignClient {

    @PostMapping("/api/v1/internal/stocks/confirm")
    ApiResponse<Void> confirmStock(@RequestBody ProductStockConfirmApiRequest request);
}
