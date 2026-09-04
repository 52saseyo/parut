package com.parut.user.auth.application.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {}