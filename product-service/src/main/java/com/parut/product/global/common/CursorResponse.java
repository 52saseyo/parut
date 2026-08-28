package com.parut.product.global.common;

import java.util.List;

public record CursorResponse<T>(
        List<T> content,
        CursorPageInfo pageInfo
) {
}
