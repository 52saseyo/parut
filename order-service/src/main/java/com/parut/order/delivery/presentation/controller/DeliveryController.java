package com.parut.order.delivery.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parut.order.delivery.application.DeliveryService;
import com.parut.order.delivery.domain.Delivery;
import com.parut.order.delivery.presentation.dto.request.StartDeliveryRequest;
import com.parut.order.delivery.presentation.dto.response.DeliveryResponse;
import com.parut.order.delivery.presentation.dto.response.StartDeliveryResponse;
import com.parut.order.global.auth.RequireRole;
import com.parut.order.global.auth.UserContext;
import com.parut.order.global.auth.UserRole;
import com.parut.order.global.common.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping
    @RequireRole(UserRole.SELLER)
    public ApiResponse<List<DeliveryResponse>> getDeliveries(
            @RequestParam UUID orderId,
            UserContext userContext
    ) {
        List<DeliveryResponse> deliveries = deliveryService
                .getDeliveries(orderId, userContext.userId(), userContext.role()).stream()
                .map(DeliveryResponse::from)
                .toList();

        return ApiResponse.success(deliveries);
    }

    @GetMapping("/{deliveryId}")
    @RequireRole({UserRole.CUSTOMER, UserRole.SELLER, UserRole.ADMIN})
    public ApiResponse<DeliveryResponse> getDelivery(
            @PathVariable UUID deliveryId,
            UserContext userContext
    ) {
        Delivery delivery = deliveryService.getDelivery(deliveryId, userContext.userId(), userContext.role());

        return ApiResponse.success(DeliveryResponse.from(delivery));
    }

    @PatchMapping("/{deliveryId}/ship")
    @RequireRole(UserRole.SELLER)
    public ApiResponse<StartDeliveryResponse> startDelivery(
            @PathVariable UUID deliveryId,
            UserContext userContext,
            @RequestBody StartDeliveryRequest request
    ) {
        Delivery delivery = deliveryService.startDelivery(
                deliveryId,
                userContext.userId(),
                userContext.role(),
                request.trackingNumber()
        );

        return ApiResponse.success(StartDeliveryResponse.from(delivery));
    }
}
