package com.parut.user.global.common;

import java.time.Instant;

public record ApiResponse<T>(
        String code,
        T data,
        String traceId,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>("OK", data, traceId, Instant.now());
    }
}
