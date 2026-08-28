package com.parut.user.global.exception;

import java.time.Instant;

public record ErrorResponse(
        boolean success,
        String code,
        String message,
        String traceId,
        Instant timestamp
) {
    public static ErrorResponse of(
            ErrorCode errorCode,
            String traceId
    ) {
        return new ErrorResponse(
                false,
                errorCode.name(),
                errorCode.getMessage(),
                traceId,
                java.time.Instant.now()
        );
    }
}
