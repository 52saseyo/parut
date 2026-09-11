package com.parut.user.seller.application.dto.response;

import java.time.ZonedDateTime;
import java.util.UUID;

public record SellerDeleteResponse(
        UUID sellerId,
        ZonedDateTime deletedAt
) {
    public static SellerDeleteResponse of(UUID sellerId, ZonedDateTime deletedAt) {
        return new SellerDeleteResponse(sellerId, deletedAt);
    }
}