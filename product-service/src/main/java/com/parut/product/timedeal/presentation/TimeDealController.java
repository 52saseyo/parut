package com.parut.product.timedeal.presentation;

import com.parut.product.global.common.ApiResponse;
import com.parut.product.global.common.CursorPageInfo;
import com.parut.product.global.common.CursorResponse;
import com.parut.product.global.common.SortDirection;
import com.parut.product.global.logging.TraceIdContext;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCursorResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailResult;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealQueryUseCase;
import com.parut.product.timedeal.presentation.dto.timedeal.response.TimeDealPublicDetailResponse;
import com.parut.product.timedeal.presentation.support.TimeDealCursorRequestValidator;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/time-deals")
@RequiredArgsConstructor
public class TimeDealController {

    private final TimeDealQueryUseCase timeDealQueryUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorResponse<TimeDealPublicDetailResponse>>> getList(
            @RequestParam(defaultValue = "ACTIVE") TimeDealStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID cursorId,
            @RequestParam(defaultValue = "10") int size
    ) {
        TimeDealCursorRequestValidator.validate(status, cursor, cursorId, size);

        TimeDealCursorResult<TimeDealPublicDetailResult> result =
                timeDealQueryUseCase.getPublicList(status, cursor, cursorId, size);
        CursorResponse<TimeDealPublicDetailResponse> response = new CursorResponse<>(
                result.content().stream()
                        .map(TimeDealPublicDetailResponse::from)
                        .toList(),
                CursorPageInfo.of(
                        result.nextCursor(),
                        result.nextIdAfter(),
                        result.hasNext(),
                        "startAt",
                        SortDirection.DESC)
        );
        return ResponseEntity.ok(ApiResponse.success(response, TraceIdContext.currentTraceId()));
    }

    @GetMapping("/{timeDealId}")
    public ResponseEntity<ApiResponse<TimeDealPublicDetailResponse>> getDetail(
            @PathVariable UUID timeDealId
    ) {
        TimeDealPublicDetailResponse timeDealPublicDetailResponse =
                TimeDealPublicDetailResponse.from(timeDealQueryUseCase.getPublicDetail(timeDealId));
        return ResponseEntity.ok(ApiResponse.success(
                timeDealPublicDetailResponse, TraceIdContext.currentTraceId()));
    }
}
