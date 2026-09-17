package com.parut.order.order.presentation.dto.request;

import com.parut.order.order.application.dto.CreateOrderCommand;
import com.parut.order.order.application.dto.OrderItemCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(

        @NotNull
        @Size(min = 1, max = 20, message = "주문 상품은 1개 이상 20개 이하만 가능합니다")
        List<@Valid OrderItemRequest> items,

        @NotNull
        @Valid
        RecipientRequest recipient,

        Boolean removeFromCart
) {
    public CreateOrderCommand toCommand(UUID userId, String idempotencyKey) {
        List<OrderItemCommand> itemCommands = items.stream()
                .map(item -> new OrderItemCommand(item.productId(), item.quantity()))
                .toList();
        return new CreateOrderCommand(
                userId,
                idempotencyKey,
                itemCommands,
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
