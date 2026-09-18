package com.parut.user.seller.application.client;

import com.parut.user.global.common.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

@FeignClient(name = "order-service", url = "${feign.order-service.url}")
public interface OrderServiceClient {

    // order-service에 만들어달라고 요청해야 할 API
    @GetMapping("/api/v1/internal/orders/getOrder")
    ApiResponse<Boolean> checkUnconfirmedOrders(@PathVariable("sellerId") UUID sellerId);
}
