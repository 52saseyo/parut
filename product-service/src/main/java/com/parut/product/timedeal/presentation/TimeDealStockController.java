package com.parut.product.timedeal.presentation;

import com.parut.product.global.common.ApiResponse;
import com.parut.product.global.common.CursorPageInfo;
import com.parut.product.global.common.CursorResponse;
import com.parut.product.global.common.SortDirection;
import com.parut.product.global.constant.HeaderConstants;
import com.parut.product.global.logging.TraceIdContext;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockAdjustResult;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockQueryResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCursorResult;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockTransferResult;
import com.parut.product.timedeal.application.port.in.timedealstock.TimeDealStockCommandUseCase;
import com.parut.product.timedeal.application.port.in.timedealstock.TimeDealStockQueryUseCase;
import com.parut.product.timedeal.presentation.dto.timedeal.response.TimeDealStockResponse;
import com.parut.product.timedeal.presentation.dto.timedealstock.request.TimeDealStockAdjustRequest;
import com.parut.product.timedeal.presentation.dto.timedealstock.request.TimeDealStockTransferRequest;
import com.parut.product.timedeal.presentation.dto.timedealstock.response.TimeDealStockAdjustResponse;
import com.parut.product.timedeal.presentation.dto.timedealstock.response.TimeDealStockTransferResponse;
import com.parut.product.timedeal.presentation.support.TimeDealCursorRequestValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seller/time-deals")
@RequiredArgsConstructor
public class TimeDealStockController {

    private final TimeDealStockQueryUseCase timeDealStockQueryUseCase;
    private final TimeDealStockCommandUseCase timeDealStockCommandUseCase;

    @GetMapping("/stocks")
    public ResponseEntity<ApiResponse<CursorResponse<TimeDealStockResponse>>> getSellerOwnedStockList(
            @RequestHeader(HeaderConstants.USER_ID) UUID sellerId,
            @RequestHeader(HeaderConstants.USER_ROLE) String requesterRole,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID cursorId,
            @RequestParam(defaultValue = "10") int size
    ) {
        TimeDealCursorRequestValidator.validateSeller(cursor, cursorId, size);
        TimeDealCursorResult<TimeDealStockQueryResult> result =
                timeDealStockQueryUseCase.getSellerOwnedTimeDealStockList(
                        sellerId, requesterRole, cursor, cursorId, size);
        CursorResponse<TimeDealStockResponse> response = new CursorResponse<>(
                result.content().stream()
                        .map(TimeDealStockResponse::from)
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

    @GetMapping("/{timeDealId}/stock")
    public ResponseEntity<ApiResponse<TimeDealStockResponse>> getStock(
            @PathVariable UUID timeDealId,
            @RequestHeader(HeaderConstants.USER_ID) UUID requesterId,
            @RequestHeader(HeaderConstants.USER_ROLE) String requesterRole
    ) {
        TimeDealStockQueryResult result = timeDealStockQueryUseCase.getStock(
                timeDealId, requesterId, requesterRole
        );
        TimeDealStockResponse response = TimeDealStockResponse.from(result);

        return ResponseEntity.ok(
                ApiResponse.success(response, TraceIdContext.currentTraceId())
        );
    }

    @PatchMapping("/{timeDealId}/stock")
    public ResponseEntity<ApiResponse<TimeDealStockAdjustResponse>> adjustStock(
            @PathVariable UUID timeDealId,
            @RequestHeader(HeaderConstants.USER_ID) UUID requesterId,
            @RequestHeader(HeaderConstants.USER_ROLE) String requesterRole,
            @Valid @RequestBody TimeDealStockAdjustRequest timeDealStockAdjustRequest
    ) {
        TimeDealStockAdjustResult timeDealStockAdjustResult = timeDealStockCommandUseCase.adjustStock(
                timeDealStockAdjustRequest.toCommand(timeDealId, requesterId, requesterRole));
        TimeDealStockAdjustResponse timeDealStockAdjustResponse =
                TimeDealStockAdjustResponse.from(timeDealStockAdjustResult);
        return ResponseEntity.ok(ApiResponse.success(
                timeDealStockAdjustResponse, TraceIdContext.currentTraceId()));
    }

    @PostMapping("/{timeDealId}/stock/transfer")
    public ResponseEntity<ApiResponse<TimeDealStockTransferResponse>> transferStock(
            @PathVariable UUID timeDealId,
            @RequestHeader(HeaderConstants.USER_ID) UUID requesterId,
            @RequestHeader(HeaderConstants.USER_ROLE) String requesterRole,
            @Valid @RequestBody TimeDealStockTransferRequest timeDealStockTransferRequest
    ) {
        TimeDealStockTransferResult timeDealStockTransferResult = timeDealStockCommandUseCase.transferStock(
                timeDealStockTransferRequest.toCommand(timeDealId, requesterId, requesterRole));
        TimeDealStockTransferResponse timeDealStockTransferResponse =
                TimeDealStockTransferResponse.from(timeDealStockTransferResult);
        return ResponseEntity.ok(ApiResponse.success(
                timeDealStockTransferResponse, TraceIdContext.currentTraceId()));
    }
}
