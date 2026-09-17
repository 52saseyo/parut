package com.parut.order.order.infrastructure.client;

import com.parut.order.global.common.ApiResponse;
import com.parut.order.global.config.InternalFeignConfig;
import com.parut.order.order.infrastructure.client.dto.ProductOrderInfoApiRequest;
import com.parut.order.order.infrastructure.client.dto.ProductOrderInfoApiResponse;
import com.parut.order.order.infrastructure.client.dto.ProductStockReserveApiRequest;
import com.parut.order.order.infrastructure.client.dto.ProductStockRestoreApiRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "product-service",
        url = "${product-service.base-url}",
        configuration = InternalFeignConfig.class
)
public interface ProductInternalFeignClient {

    @PostMapping("/api/v1/internal/products/order-info")
    ApiResponse<List<ProductOrderInfoApiResponse>> getOrderInfos(@RequestBody ProductOrderInfoApiRequest request);

    @PostMapping("/api/v1/internal/stocks/reserve")
    ApiResponse<Void> reserveStock(@RequestBody ProductStockReserveApiRequest request);

    @PostMapping("/api/v1/internal/stocks/restore")
    ApiResponse<Void> restoreStock(@RequestBody ProductStockRestoreApiRequest request);
}
