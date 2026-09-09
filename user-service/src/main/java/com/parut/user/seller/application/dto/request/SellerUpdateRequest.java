package com.parut.user.seller.application.dto.request;

public record SellerUpdateRequest(
        String companyName,
        String bizAddress,
        String managerName,
        String managerPhone,
        String managerEmail,
        String slackId
) {}