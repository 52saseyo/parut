package com.parut.order.refund.application;

import java.util.List;
import java.util.UUID;

import com.parut.order.refund.domain.Refund;

public record RefundPage(
        List<Refund> content,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext
) {
}
