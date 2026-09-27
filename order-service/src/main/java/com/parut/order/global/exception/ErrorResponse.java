package com.parut.order.global.exception;

import java.time.Instant;

public record ErrorResponse(
        boolean success,
        String code,
        String message,
        String traceId,
        Instant timestamp
) {
    public static ErrorResponse of(
            String code,
            String message,
            String traceId
    ) {
        return new ErrorResponse(
                false,
                code,
                message,
                traceId,
                Instant.now()
        );
    }
}
