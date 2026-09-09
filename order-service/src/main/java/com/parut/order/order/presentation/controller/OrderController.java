package com.parut.order.order.presentation.controller;

import com.parut.order.global.common.ApiResponse;
import com.parut.order.global.constant.HeaderConstants;
import com.parut.order.order.application.OrderFacade;
import com.parut.order.order.application.OrderItemConfirmationService;
import com.parut.order.order.application.OrderService;
import com.parut.order.order.application.dto.CreateOrderCommand;
import com.parut.order.order.application.dto.CreateTimeDealOrderCommand;
import com.parut.order.order.application.dto.OrderDetailData;
import com.parut.order.order.domain.Order;
import com.parut.order.order.domain.OrderItem;
import com.parut.order.order.presentation.dto.request.CreateOrderRequest;
import com.parut.order.order.presentation.dto.request.CreateTimeDealOrderRequest;
import com.parut.order.order.presentation.dto.response.OrderCreateResponse;
import com.parut.order.order.presentation.dto.response.OrderDetailResponse;
import com.parut.order.order.presentation.dto.response.OrderItemConfirmationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderFacade orderFacade;
    private final OrderService orderService;
    private final OrderItemConfirmationService orderItemConfirmationService;

    @PostMapping
    public ApiResponse<OrderCreateResponse> createOrder(
            // TODO: 공통 인터셉터 개발시, userId, traceId 부분 수정 예정
            @RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.IDEMPOTENCY_KEY) String idempotencyKey,
            @RequestHeader(value = HeaderConstants.TRACE_ID, required = false) String traceId,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        CreateOrderCommand command = request.toCommand(userId, idempotencyKey);

        Order order = orderFacade.createOrder(command);

        return ApiResponse.success(OrderCreateResponse.from(order), traceId);
    }

    @PostMapping("/time-deals")
    public ApiResponse<OrderCreateResponse> createTimeDealOrder(
            // TODO: 공통 인터셉터 개발시, userId, traceId 부분 수정 예정
            @RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.IDEMPOTENCY_KEY) String idempotencyKey,
            @RequestHeader(value = HeaderConstants.TRACE_ID, required = false) String traceId,
            @Valid @RequestBody CreateTimeDealOrderRequest request
    ) {
        CreateTimeDealOrderCommand command = request.toCommand(userId, idempotencyKey);

        Order order = orderFacade.createTimeDealOrder(command);

        return ApiResponse.success(OrderCreateResponse.from(order), traceId);
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> getOrder(
            // TODO: 공통 인터셉터 개발시, userId, userRole, traceId 부분 수정 예정
            @PathVariable UUID orderId,
            @RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestHeader(value = HeaderConstants.TRACE_ID, required = false) String traceId
    ) {
        OrderDetailData detail = orderService.getOrderDetail(orderId, userId, userRole);

        return ApiResponse.success(OrderDetailResponse.from(detail), traceId);
    }

    @PatchMapping("/{orderId}/items/{orderItemId}/confirm")
    public ApiResponse<OrderItemConfirmationResponse> confirmOrderItem(
            @PathVariable UUID orderId,
            @PathVariable UUID orderItemId,
            @RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(value = HeaderConstants.TRACE_ID, required = false) String traceId
    ) {
        OrderItem orderItem = orderItemConfirmationService.confirmOrderItem(orderId, orderItemId, userId);

        return ApiResponse.success(OrderItemConfirmationResponse.from(orderItem), traceId);
    }
}
