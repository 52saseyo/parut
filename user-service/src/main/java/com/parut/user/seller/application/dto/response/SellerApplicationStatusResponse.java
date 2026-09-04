package com.parut.user.seller.application.dto.response;

import com.parut.user.seller.domain.Seller;
import com.parut.user.seller.domain.SellerStatus;
import java.time.ZonedDateTime;

public record SellerApplicationStatusResponse(
        SellerStatus status,
        String rejectReason,
        ZonedDateTime approvedAt
) {
    public static SellerApplicationStatusResponse from(Seller seller) {
        return new SellerApplicationStatusResponse(
                seller.getStatus(),
                seller.getRejectReason(),
                seller.getApprovedAt()
        );
    }
}