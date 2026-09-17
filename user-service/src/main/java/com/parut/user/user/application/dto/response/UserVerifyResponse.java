package com.parut.user.user.application.dto.response;

public record UserVerifyResponse(
        boolean isValid,
        String userId,
        String role
) {
    public static UserVerifyResponse success(String userId, String role) {
        return new UserVerifyResponse(true, userId, role);
    }

    public static UserVerifyResponse fail() {
        return new UserVerifyResponse(false, null, null);
    }
}
