package com.parut.order.global.common;

import com.parut.order.global.filter.TraceIdFilter;

import java.time.Instant;

public record ApiResponse<T>(
        String code,
        T data,
        String traceId,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("OK", data, TraceIdFilter.currentTraceId(), Instant.now());
    }
}
