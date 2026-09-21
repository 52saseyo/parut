package com.parut.product.timedeal.presentation.dto.timedeal.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record TimeDealBulkDetailRequest(
        @NotEmpty(message = "타임딜 ID 목록은 비어 있을 수 없습니다.")
        @Size(max = 100, message = "한 번에 최대 100개의 타임딜만 조회할 수 있습니다.")
        List<@NotNull UUID> timeDealIds
) {
}
