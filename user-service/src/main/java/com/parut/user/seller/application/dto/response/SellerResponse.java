package com.parut.user.seller.application.dto.response;

import com.parut.user.seller.domain.Seller;
import com.parut.user.seller.domain.SellerStatus;
import java.time.ZonedDateTime;
import java.util.UUID;

public record SellerResponse(
        UUID id,
        String loginId,
        String companyName,
        String bizRegNo,
        String repName,
        String bizAddress,
        String managerName,
        String managerPhone,
        String managerEmail,
        String slackId,
        SellerStatus status,
        ZonedDateTime createdAt
) {
    public static SellerResponse from(Seller seller) {
        return new SellerResponse(
                seller.getId(),
                seller.getLoginId(),
                seller.getCompanyName(),
                seller.getBizRegNo(),
                seller.getRepName(),
                seller.getBizAddress(),
                seller.getManagerName(),
                seller.getManagerPhone(),
                seller.getManagerEmail(),
                seller.getSlackId(),
                seller.getStatus(),
                seller.getCreatedAt()
        );
    }
}