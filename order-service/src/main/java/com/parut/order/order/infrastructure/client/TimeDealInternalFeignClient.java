package com.parut.order.order.infrastructure.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.parut.order.global.common.ApiResponse;
import com.parut.order.global.config.InternalFeignConfig;
import com.parut.order.global.constant.HeaderConstants;
import com.parut.order.order.infrastructure.client.dto.TimeDealInfoApiResponse;
import com.parut.order.order.infrastructure.client.dto.TimeDealPurchaseCancelApiRequest;
import com.parut.order.order.infrastructure.client.dto.TimeDealPurchaseReserveApiRequest;

@FeignClient(
        name = "product-service",
        contextId = "timeDealInternalFeignClient",
        url = "${product-service.base-url}",
        configuration = InternalFeignConfig.class
)
public interface TimeDealInternalFeignClient {

    @GetMapping("/api/v1/internal/time-deals/{timeDealId}")
    ApiResponse<TimeDealInfoApiResponse> getOrderInfo(@PathVariable("timeDealId") UUID timeDealId);

    @PostMapping("/api/v1/internal/time-deals/{timeDealId}/purchases")
    void reserveStock(
            @PathVariable("timeDealId") UUID timeDealId,
            @RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestBody TimeDealPurchaseReserveApiRequest request
    );

    @PostMapping("/api/v1/internal/time-deal-purchases/{orderId}/cancel")
    void restoreStock(
            @PathVariable("orderId") UUID orderId,
            @RequestBody TimeDealPurchaseCancelApiRequest request
    );
}
