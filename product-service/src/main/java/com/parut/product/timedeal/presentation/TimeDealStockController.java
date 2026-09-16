package com.parut.product.timedeal.presentation;

import com.parut.product.global.common.ApiResponse;
import com.parut.product.global.logging.TraceIdContext;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockQueryResult;
import com.parut.product.timedeal.application.port.in.timedealstock.TimeDealStockQueryUseCase;
import com.parut.product.timedeal.presentation.dto.timedeal.response.TimeDealStockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
            @PathVariable UUID timeDealId
    ) {
        TimeDealStockQueryResult result = timeDealStockQueryUseCase.getStock(timeDealId);
        TimeDealStockResponse response = TimeDealStockResponse.from(result);

        return ResponseEntity.ok(
                ApiResponse.success(response, TraceIdContext.currentTraceId())
        );
    }
}
