package com.parut.order.order.presentation.dto.response;

public record SellerUnprocessedOrderExistsResponse(
        boolean exists
) {
    public static SellerUnprocessedOrderExistsResponse of(boolean exists) {
        return new SellerUnprocessedOrderExistsResponse(exists);
    }
}
