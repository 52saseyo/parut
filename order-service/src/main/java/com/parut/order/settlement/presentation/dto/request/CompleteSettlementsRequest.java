package com.parut.order.settlement.presentation.dto.request;

import java.util.List;
import java.util.UUID;

public record CompleteSettlementsRequest(List<UUID> settlementIds) {
}
