package com.parut.product.product.application.stock.dto;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.UUID;

public record HistoryCursor(
        Instant eventLogCreatedAt, UUID eventLogId,
        Instant allocationCreatedAt, UUID allocationId
) {
    public String encode() {
        String raw = String.join("|",
                str(eventLogCreatedAt), str(eventLogId),
                str(allocationCreatedAt), str(allocationId));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static HistoryCursor decode(String encoded) {
        if (encoded == null) return new HistoryCursor(null, null, null, null);
        try {
            String[] p = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8).split("\\|", -1);
            if (p.length != 4) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            return new HistoryCursor(
                    p[0].isEmpty() ? null : Instant.parse(p[0]),
                    p[1].isEmpty() ? null : UUID.fromString(p[1]),
                    p[2].isEmpty() ? null : Instant.parse(p[2]),
                    p[3].isEmpty() ? null : UUID.fromString(p[3])
            );
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
    private static String str(Object o) { return o == null ? "" : o.toString(); }
}