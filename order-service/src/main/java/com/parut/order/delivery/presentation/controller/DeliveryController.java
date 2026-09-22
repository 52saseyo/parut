package com.parut.order.delivery.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parut.order.delivery.application.DeliveryPage;
import com.parut.order.delivery.application.DeliveryService;
import com.parut.order.delivery.domain.Delivery;
import com.parut.order.delivery.domain.DeliveryStatus;
import com.parut.order.delivery.presentation.dto.request.StartDeliveryRequest;
import com.parut.order.delivery.presentation.dto.response.AdminDeliveryResponse;
import com.parut.order.delivery.presentation.dto.response.DeliveryResponse;
import com.parut.order.delivery.presentation.dto.response.StartDeliveryResponse;
import com.parut.order.global.auth.RequireRole;
import com.parut.order.global.auth.UserContext;
import com.parut.order.global.auth.UserRole;
import com.parut.order.global.common.ApiResponse;
import com.parut.order.global.common.CursorPageInfo;
import com.parut.order.global.common.CursorResponse;
import com.parut.order.global.common.OffsetPageInfo;
import com.parut.order.global.common.OffsetResponse;
import com.parut.order.global.common.SortDirection;

import lombok.RequiredArgsConstructor;

/** 외부 사용자의 배송 조회와 판매자의 배송 시작 API를 제공한다. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    /** 고객과 판매자의 소유 범위에 속한 배송 목록을 커서 방식으로 제공한다. */
    @GetMapping("/deliveries")
    @RequireRole({UserRole.CUSTOMER, UserRole.SELLER})
    public ApiResponse<CursorResponse<DeliveryResponse>> getDeliveries(
            UserContext userContext,
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) DeliveryStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID cursorId,
            @RequestParam(defaultValue = "10") int size
    ) {
        DeliveryPage page = deliveryService.getDeliveries(
                userContext.userId(), userContext.role(), orderId, status, cursor, cursorId, size);
        List<DeliveryResponse> content = page.content().stream()
                .map(DeliveryResponse::from)
                .toList();
        CursorPageInfo pageInfo = CursorPageInfo.of(
                page.nextCursor(), page.nextIdAfter(), page.hasNext(), "createdAt", SortDirection.DESC);

        return ApiResponse.success(new CursorResponse<>(content, pageInfo));
    }

    /** 관리 화면의 전체 건수와 페이지 이동을 위해 오프셋 페이지를 제공한다. */
    @GetMapping("/admin/deliveries")
    @RequireRole(UserRole.ADMIN)
    public ApiResponse<OffsetResponse<AdminDeliveryResponse>> getAdminDeliveries(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) DeliveryStatus status,
            @PageableDefault(
                    size = 10,
                    sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        Page<Delivery> page = deliveryService
                .getAdminDeliveries(customerId, sellerId, orderId, status, pageable);
        List<AdminDeliveryResponse> content = page.getContent().stream()
                .map(AdminDeliveryResponse::from)
                .toList();
        Sort.Order sortOrder = page.getSort().stream()
                .findFirst()
                .orElse(Sort.Order.desc("createdAt"));
        OffsetPageInfo pageInfo = OffsetPageInfo.of(
                page.getNumber(),
                page.getSize(),
                sortOrder.getProperty(),
                sortOrder.isAscending() ? SortDirection.ASC : SortDirection.DESC,
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );

        return ApiResponse.success(new OffsetResponse<>(content, pageInfo));
    }

    @GetMapping("/deliveries/{deliveryId}")
    @RequireRole({UserRole.CUSTOMER, UserRole.SELLER, UserRole.ADMIN})
    public ApiResponse<DeliveryResponse> getDelivery(
            @PathVariable UUID deliveryId,
            UserContext userContext
    ) {
        Delivery delivery = deliveryService.getDelivery(deliveryId, userContext.userId(), userContext.role());

        return ApiResponse.success(DeliveryResponse.from(delivery));
    }

    @PatchMapping("/deliveries/{deliveryId}/ship")
    @RequireRole(UserRole.SELLER)
    public ApiResponse<StartDeliveryResponse> startDelivery(
            @PathVariable UUID deliveryId,
            UserContext userContext,
            @RequestBody StartDeliveryRequest request
    ) {
        Delivery delivery = deliveryService.startDelivery(
                deliveryId,
                userContext.userId(),
                request.trackingNumber()
        );

        return ApiResponse.success(StartDeliveryResponse.from(delivery));
    }
}
