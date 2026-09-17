package com.parut.order.refund.presentation.dto.request;

import java.util.List;
import java.util.UUID;

public record ApproveRefundRequest(
        List<UUID> refundIds
) {
}