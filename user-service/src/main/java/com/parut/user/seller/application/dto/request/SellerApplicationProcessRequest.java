package com.parut.user.seller.application.dto.request;

import com.parut.user.seller.domain.SellerStatus;

public record SellerApplicationProcessRequest(
        SellerStatus status,
        String rejectReason
) {}