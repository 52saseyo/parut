package com.parut.order.order.application;

import com.parut.order.order.application.dto.OrderItemSummary;

import java.util.List;
import java.util.UUID;

public record OrderItemPage(
        List<OrderItemSummary> content,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext
) {
}
