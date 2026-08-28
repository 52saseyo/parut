package com.parut.product.global.common;

import java.util.List;

public record OffsetResponse<T>(
        List<T> content,
        OffsetPageInfo pageInfo
) {
}
