package com.parut.order.delivery.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parut.order.delivery.application.DeliveryService;
import com.parut.order.delivery.domain.Delivery;
import com.parut.order.delivery.presentation.dto.request.StartDeliveryRequest;
import com.parut.order.delivery.presentation.dto.response.DeliveryResponse;
import com.parut.order.delivery.presentation.dto.response.StartDeliveryResponse;
import com.parut.order.global.common.ApiResponse;
import com.parut.order.global.constant.HeaderConstants;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping
    public ApiResponse<List<DeliveryResponse>> getDeliveries(
            @RequestParam UUID orderId,
            @RequestHeader(HeaderConstants.USER_ID) UUID sellerId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.TRACE_ID, required = false) String traceId
    ) {
        List<DeliveryResponse> deliveries = deliveryService.getDeliveries(orderId, sellerId, userRole).stream()
                .map(DeliveryResponse::from)
                .toList();

        return ApiResponse.success(deliveries, traceId);
    }

    @GetMapping("/{deliveryId}")
    public ApiResponse<DeliveryResponse> getDelivery(
            @PathVariable UUID deliveryId,
            @RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.TRACE_ID, required = false) String traceId
    ) {
        Delivery delivery = deliveryService.getDelivery(deliveryId, userId, userRole);
        return ApiResponse.success(DeliveryResponse.from(delivery), traceId);
    }

    @PatchMapping("/{deliveryId}/ship")
    public ApiResponse<StartDeliveryResponse> startDelivery(
            @PathVariable UUID deliveryId,
            @RequestHeader(HeaderConstants.USER_ID) UUID sellerId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.TRACE_ID, required = false) String traceId,
            @RequestBody StartDeliveryRequest request
    ) {
        Delivery delivery = deliveryService.startDelivery(
                deliveryId,
                sellerId,
                userRole,
                request.trackingNumber()
        );

        return ApiResponse.success(StartDeliveryResponse.from(delivery), traceId);
    }
}
