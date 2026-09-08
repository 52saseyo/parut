package com.parut.product.timedeal.presentation;

import com.parut.product.global.constant.HeaderConstants;
import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseConfirmCommand;
import com.parut.product.timedeal.application.port.in.timedealpurchase.TimeDealPurchaseCommandUseCase;
import com.parut.product.timedeal.presentation.dto.timedealpurchase.request.TimeDealPurchaseCancelRequest;
import com.parut.product.timedeal.presentation.dto.timedealpurchase.request.TimeDealPurchaseReserveRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalTimeDealController {

    private final TimeDealPurchaseCommandUseCase timeDealPurchaseCommandUseCase;


    @PostMapping("/time-deals/{timeDealId}/purchases")
    public ResponseEntity<Void> reserve(
            @PathVariable UUID timeDealId,
            @RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @Valid @RequestBody TimeDealPurchaseReserveRequest timeDealPurchaseReserveRequest
    ) {
        timeDealPurchaseCommandUseCase.reserve(
                timeDealPurchaseReserveRequest.toCommand(timeDealId, userId));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @PostMapping("/time-deal-purchases/{orderId}/confirm")
    public ResponseEntity<Void> confirm(
            @PathVariable UUID orderId
    ) {
        timeDealPurchaseCommandUseCase.confirm(new TimeDealPurchaseConfirmCommand(orderId));
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/time-deal-purchases/{orderId}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID orderId,
            @RequestBody TimeDealPurchaseCancelRequest timeDealPurchaseCancelRequest
    ) {
        timeDealPurchaseCommandUseCase.cancel(timeDealPurchaseCancelRequest.toCommand(orderId));
        return ResponseEntity.noContent().build();
    }
}
