package com.parut.order.order.presentation.dto.request;

import com.parut.order.order.application.dto.CreateOrderCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(

        // ToDo: bulk 고려 시점에 수정 예정
        @NotNull
        @Size(min = 1, max = 1, message = "MVP는 단일 상품 주문만 지원")
        List<@Valid OrderItemRequest> items,

        @NotNull
        @Valid
        RecipientRequest recipient,

        Boolean removeFromCart
) {
    public CreateOrderCommand toCommand(UUID userId, String idempotencyKey) {
        OrderItemRequest item = items.get(0);
        return new CreateOrderCommand(
                userId,
                idempotencyKey,
                item.productId(),
                item.quantity(),
                recipient.recipientName(),
                recipient.recipientPhone(),
                recipient.zipCode(),
                recipient.addressBase(),
                recipient.addressDetail(),
                recipient.deliveryRequest()
        );
    }

    public record OrderItemRequest(
            @NotNull
            UUID productId,

            @NotNull
            @Min(1)
            Integer quantity
    ) {
    }

    public record RecipientRequest(
            @NotBlank
            String recipientName,

            @NotBlank
            String recipientPhone,

            @NotBlank
            String zipCode,

            @NotBlank
            String addressBase,

            String addressDetail,
            String deliveryRequest
    ) {
    }
}
