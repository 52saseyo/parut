package com.parut.user.global.common;

public record OffsetPageInfo(
        PaginationType paginationType,
        int page,
        int size,
        String sort,
        SortDirection direction,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static OffsetPageInfo of(
            int page,
            int size,
            String sort,
            SortDirection direction,
            long totalElements,
            int totalPages,
            boolean last
    ) {
        return new OffsetPageInfo(
                PaginationType.OFFSET,
                page,
                size,
                sort,
                direction,
                totalElements,
                totalPages,
                last
        );
    }

}
