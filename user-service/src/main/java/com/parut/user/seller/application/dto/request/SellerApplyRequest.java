package com.parut.user.seller.application.dto.request;

public record SellerApplyRequest(
        String loginId,
        String password,
        String companyName,
        String bizRegNo,
        String repName,
        String bizAddress,
        String managerName,
        String managerPhone,
        String managerEmail,
        String slackId
) {}