package com.parut.order.global.common;

import java.util.UUID;

public record CursorPageInfo(
        PaginationType paginationType,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        String sortBy,
        SortDirection sortDirection
) {
    public static CursorPageInfo of(
            String nextCursor,
            UUID nextIdAfter,
            boolean hasNext,
            String sortBy,
            SortDirection sortDirection
    ) {
        return new CursorPageInfo(
                PaginationType.CURSOR,
                nextCursor,
                nextIdAfter,
                hasNext,
                sortBy,
                sortDirection
        );
    }
}