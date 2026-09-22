package com.parut.order.order.infrastructure.client.dto;

import java.util.List;
import java.util.UUID;

public record TimeDealBulkDetailApiRequest(
        List<UUID> timeDealIds
) {
}
