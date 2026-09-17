package com.parut.product.timedeal.application.dto.timedeal;

import java.util.List;
import java.util.UUID;

public record TimeDealCursorResult<T>(
        List<T> content,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext
) {
    public static <T> TimeDealCursorResult<T> of(
            List<T> content,
            String nextCursor,
            UUID nextIdAfter,
            boolean hasNext
    ) {
        return new TimeDealCursorResult<>(content, nextCursor, nextIdAfter, hasNext);
    }

    public static <T> TimeDealCursorResult<T> withoutNextCursor(List<T> content) {
        return new TimeDealCursorResult<>(content, null, null, false);
    }
}
