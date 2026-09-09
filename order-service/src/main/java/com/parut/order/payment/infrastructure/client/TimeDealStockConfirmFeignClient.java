package com.parut.order.payment.infrastructure.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.parut.order.global.config.InternalFeignConfig;

@FeignClient(
        name = "product-service",
        contextId = "timeDealStockConfirmFeignClient",
        url = "${product-service.base-url}",
        configuration = InternalFeignConfig.class
)
public interface TimeDealStockConfirmFeignClient {

    @PostMapping("/api/v1/internal/time-deal-purchases/{orderId}/confirm")
    void confirmStock(@PathVariable("orderId") UUID orderId);
}
