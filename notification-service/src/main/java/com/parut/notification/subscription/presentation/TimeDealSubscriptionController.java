package com.parut.notification.subscription.presentation;

import com.parut.notification.global.common.ApiResponse;
import com.parut.notification.global.constant.HeaderConstants;
import com.parut.notification.global.logging.TraceIdContext;
import com.parut.notification.subscription.application.dto.TimeDealSubscriptionResult;
import com.parut.notification.subscription.application.port.in.TimeDealSubscriptionCommandUseCase;
import com.parut.notification.subscription.application.port.in.TimeDealSubscriptionQueryUseCase;
import com.parut.notification.subscription.presentation.dto.request.SubscribeTimeDealRequest;
import com.parut.notification.subscription.presentation.dto.response.TimeDealSubscriptionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notification-subscriptions")
public class TimeDealSubscriptionController {
    private final TimeDealSubscriptionCommandUseCase commandUseCase;
    private final TimeDealSubscriptionQueryUseCase queryUseCase;

    @PostMapping("/time-deals")
    public ResponseEntity<ApiResponse<TimeDealSubscriptionResponse>> subscribe(
            @RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @Valid @RequestBody SubscribeTimeDealRequest request
    ){
        TimeDealSubscriptionResult result =
                commandUseCase.subscribe(request.toCommand(userId, userRole));

        TimeDealSubscriptionResponse response = TimeDealSubscriptionResponse.from(result);
        return ResponseEntity.ok(ApiResponse.success(response, TraceIdContext.currentTraceId()));
    }

    @PatchMapping("/time-deals/{timeDealId}/unsubscribe")
    public ResponseEntity<ApiResponse<Void>> unsubscribe(
            @RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @PathVariable UUID timeDealId
    ){
        commandUseCase.unsubscribe(userId, userRole, timeDealId);

        return ResponseEntity.ok(ApiResponse.success(null, TraceIdContext.currentTraceId()));
    }

    @GetMapping("/time-deals/{timeDealId}")
    public ResponseEntity<ApiResponse<TimeDealSubscriptionResponse>> getSubscription(
            @RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @PathVariable UUID timeDealId
    ){
        TimeDealSubscriptionResult result =
                queryUseCase.getSubscriptionStatus(userId, userRole, timeDealId);

        TimeDealSubscriptionResponse response = TimeDealSubscriptionResponse.from(result);
        return ResponseEntity.ok(ApiResponse.success(response, TraceIdContext.currentTraceId()));
    }
}
