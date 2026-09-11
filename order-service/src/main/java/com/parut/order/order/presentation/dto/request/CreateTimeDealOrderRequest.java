package com.parut.order.order.presentation.dto.request;

import com.parut.order.order.application.dto.CreateTimeDealOrderCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTimeDealOrderRequest(
        @NotNull
        UUID timeDealId,

        @NotNull
        UUID productId,

        @NotNull
        @Min(1)
        Integer quantity,

        @NotNull
        @Valid
        RecipientRequest recipient
) {
    public CreateTimeDealOrderCommand toCommand(UUID userId, String idempotencyKey) {
        return new CreateTimeDealOrderCommand(
                userId,
                idempotencyKey,
                timeDealId,
                productId,
                quantity,
                recipient.recipientName(),
                recipient.recipientPhone(),
                recipient.zipCode(),
                recipient.addressBase(),
                recipient.addressDetail(),
                recipient.deliveryRequest()
        );
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
