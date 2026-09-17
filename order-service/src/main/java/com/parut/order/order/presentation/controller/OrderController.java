package com.parut.order.order.presentation.controller;

import com.parut.order.global.auth.RequireRole;
import com.parut.order.global.auth.UserContext;
import com.parut.order.global.auth.UserRole;
import com.parut.order.global.common.ApiResponse;
import com.parut.order.global.constant.HeaderConstants;
import com.parut.order.order.application.OrderCancelFacade;
import com.parut.order.order.application.OrderFacade;
import com.parut.order.order.application.OrderItemConfirmationService;
import com.parut.order.order.application.OrderService;
import com.parut.order.order.application.dto.CreateOrderCommand;
import com.parut.order.order.application.dto.CreateTimeDealOrderCommand;
import com.parut.order.order.application.dto.OrderCancelResult;
import com.parut.order.order.application.dto.OrderDetailData;
import com.parut.order.order.domain.Order;
import com.parut.order.order.domain.OrderItem;
import com.parut.order.order.presentation.dto.request.CancelOrderRequest;
import com.parut.order.order.presentation.dto.request.CreateOrderRequest;
import com.parut.order.order.presentation.dto.request.CreateTimeDealOrderRequest;
import com.parut.order.order.presentation.dto.response.OrderCancelResponse;
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
    private final OrderCancelFacade orderCancelFacade;
    private final OrderService orderService;
    private final OrderItemConfirmationService orderItemConfirmationService;

    @PostMapping
    @RequireRole(UserRole.CUSTOMER)
    public ApiResponse<OrderCreateResponse> createOrder(
            UserContext userContext,
            @RequestHeader(HeaderConstants.IDEMPOTENCY_KEY) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        CreateOrderCommand command = request.toCommand(userContext.userId(), idempotencyKey);

        Order order = orderFacade.createOrder(command);

        return ApiResponse.success(OrderCreateResponse.from(order));
    }

    @PostMapping("/time-deals")
    @RequireRole(UserRole.CUSTOMER)
    public ApiResponse<OrderCreateResponse> createTimeDealOrder(
            UserContext userContext,
            @RequestHeader(HeaderConstants.IDEMPOTENCY_KEY) String idempotencyKey,
            @Valid @RequestBody CreateTimeDealOrderRequest request
    ) {
        CreateTimeDealOrderCommand command = request.toCommand(userContext.userId(), idempotencyKey);

        Order order = orderFacade.createTimeDealOrder(command);

        return ApiResponse.success(OrderCreateResponse.from(order));
    }

    @GetMapping("/{orderId}")
    @RequireRole({UserRole.CUSTOMER, UserRole.ADMIN})
    public ApiResponse<OrderDetailResponse> getOrder(
            @PathVariable UUID orderId,
            UserContext userContext
    ) {
        OrderDetailData detail = orderService.getOrderDetail(orderId, userContext.userId(), userContext.role());

        return ApiResponse.success(OrderDetailResponse.from(detail));
    }

    @PostMapping("/{orderId}/cancel")
    @RequireRole({UserRole.CUSTOMER, UserRole.SELLER})
    public ApiResponse<OrderCancelResponse> cancelOrder(
            @PathVariable UUID orderId,
            UserContext userContext,
            @RequestHeader(HeaderConstants.IDEMPOTENCY_KEY) String idempotencyKey,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        OrderCancelResult result = orderCancelFacade.cancel(request.toCommand(orderId, userContext, idempotencyKey));

        return ApiResponse.success(OrderCancelResponse.from(result));
    }

    @PatchMapping("/{orderId}/items/{orderItemId}/confirm")
    @RequireRole(UserRole.CUSTOMER)
    public ApiResponse<OrderItemConfirmationResponse> confirmOrderItem(
            @PathVariable UUID orderId,
            @PathVariable UUID orderItemId,
            UserContext userContext
    ) {
        OrderItem orderItem = orderItemConfirmationService.confirmOrderItem(orderId, orderItemId, userContext.userId());

        return ApiResponse.success(OrderItemConfirmationResponse.from(orderItem));
    }
}
