package com.parut.user.seller.application.client;

import com.parut.user.global.common.ApiResponse;
import com.parut.user.seller.application.dto.response.UnprocessedOrderExistsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "order-service", url = "${order-service.base-url}")
public interface OrderServiceClient {

    @GetMapping("/api/v1/internal/orders/unprocessed")
    ApiResponse<UnprocessedOrderExistsResponse> checkUnconfirmedOrders(
            @RequestHeader("X-Service-Key") String serviceKey,
            @RequestHeader("X-Trace-Id") String traceId,
            @RequestParam("sellerId") UUID sellerId
    );
}
