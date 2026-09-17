package com.parut.product.timedeal.presentation;

import com.parut.product.global.common.ApiResponse;
import com.parut.product.global.constant.HeaderConstants;
import com.parut.product.global.logging.TraceIdContext;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockQueryResult;
import com.parut.product.timedeal.application.port.in.timedealstock.TimeDealStockQueryUseCase;
import com.parut.product.timedeal.presentation.dto.timedeal.response.TimeDealStockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockAdjustResult;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockTransferResult;
import com.parut.product.timedeal.application.port.in.timedealstock.TimeDealStockCommandUseCase;
import com.parut.product.timedeal.presentation.dto.timedealstock.request.TimeDealStockAdjustRequest;
import com.parut.product.timedeal.presentation.dto.timedealstock.request.TimeDealStockTransferRequest;
import com.parut.product.timedeal.presentation.dto.timedealstock.response.TimeDealStockAdjustResponse;
import com.parut.product.timedeal.presentation.dto.timedealstock.response.TimeDealStockTransferResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/time-deals")
@RequiredArgsConstructor
public class TimeDealStockController {

    private final TimeDealStockQueryUseCase timeDealStockQueryUseCase;

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
    private final TimeDealStockCommandUseCase timeDealStockCommandUseCase;

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
