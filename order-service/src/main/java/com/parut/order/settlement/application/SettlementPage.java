package com.parut.order.settlement.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.parut.order.settlement.domain.Settlement;

/** Settlement 조회 결과와 다음 페이지를 시작할 Cursor를 전달한다. */
public record SettlementPage(
        List<Settlement> content,
        Instant nextCursor,
        UUID nextIdAfter,
        boolean hasNext
) {
}
