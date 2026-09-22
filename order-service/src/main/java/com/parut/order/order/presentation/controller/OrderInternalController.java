package com.parut.order.order.presentation.controller;

import com.parut.order.global.common.ApiResponse;
import com.parut.order.order.application.port.in.SellerUnprocessedOrderQueryUseCase;
import com.parut.order.order.presentation.dto.response.SellerUnprocessedOrderExistsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/orders")
@RequiredArgsConstructor
public class OrderInternalController {

    private final SellerUnprocessedOrderQueryUseCase sellerUnprocessedOrderQueryUseCase;

    @GetMapping("/unprocessed")
    public ApiResponse<SellerUnprocessedOrderExistsResponse> hasUnprocessedOrder(
            @RequestParam UUID sellerId
    ) {
        boolean exists = sellerUnprocessedOrderQueryUseCase.hasUnprocessedOrder(sellerId);

        return ApiResponse.success(SellerUnprocessedOrderExistsResponse.of(exists));
    }
}
