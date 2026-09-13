package com.parut.product.timedeal.presentation;

import com.parut.product.global.common.ApiResponse;
import com.parut.product.global.constant.HeaderConstants;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealUpdateResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealDeleteCommand;
import com.parut.product.timedeal.presentation.dto.timedeal.response.TimeDealUpdateResponse;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealCommandUseCase;
import com.parut.product.timedeal.presentation.dto.timedeal.request.TimeDealConvertRequest;
import com.parut.product.timedeal.presentation.dto.timedeal.request.TimeDealCreateRequest;
import com.parut.product.timedeal.presentation.dto.timedeal.request.TimeDealUpdateRequest;
import com.parut.product.timedeal.presentation.dto.timedeal.response.TimeDealCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/time-deals")
@RequiredArgsConstructor
public class TimeDealController {

    private final TimeDealCommandUseCase timeDealCommandUseCase;

    @DeleteMapping("/{timeDealId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID timeDealId,
            @RequestHeader(HeaderConstants.USER_ID) UUID requesterId,
            @RequestHeader(HeaderConstants.USER_ROLE) String requesterRole,
            @RequestHeader(value = HeaderConstants.TRACE_ID, required = false) String traceId
    ) {
        timeDealCommandUseCase.delete(new TimeDealDeleteCommand(timeDealId, requesterId, requesterRole));
        return ResponseEntity.ok(ApiResponse.success(null, traceId));
    }

    @PatchMapping("/{timeDealId}")
    public ResponseEntity<ApiResponse<TimeDealUpdateResponse>> update(
            @PathVariable UUID timeDealId,
            @RequestHeader(HeaderConstants.USER_ID) UUID requesterId,
            @RequestHeader(HeaderConstants.USER_ROLE) String requesterRole,
            @RequestHeader(value = HeaderConstants.TRACE_ID, required = false) String traceId,
            @Valid @RequestBody TimeDealUpdateRequest request
    ) {
        TimeDealUpdateResult result =
                timeDealCommandUseCase.update(request.toCommand(timeDealId, requesterId, requesterRole));
        return ResponseEntity.ok(ApiResponse.success(TimeDealUpdateResponse.from(result), traceId));
    }


    // NOTE: 판매자 직접 등록. 일반 상품에서 전환하는 경로는 별도 엔드포인트로 붙는다.
    @PostMapping
    public ResponseEntity<ApiResponse<TimeDealCreateResponse>> create(
            @RequestHeader(HeaderConstants.USER_ID) UUID sellerId,
            @RequestHeader(HeaderConstants.USER_ROLE) String requesterRole,
            @RequestHeader(value = HeaderConstants.TRACE_ID, required = false) String traceId,
            @Valid @RequestBody TimeDealCreateRequest timeDealCreateRequest
    ) {
        TimeDealCreateResult timeDealCreateResult =
                timeDealCommandUseCase.create(timeDealCreateRequest.toCommand(sellerId, requesterRole));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(TimeDealCreateResponse.from(timeDealCreateResult), traceId));
    }


    // NOTE: 일반 상품 전환. 요청자가 판매자 본인이 아닐 수 있어(운영자) role까지 넘긴다 — 소유권 판정은 product 몫이다.
    @PostMapping("/conversions")
    public ResponseEntity<ApiResponse<TimeDealCreateResponse>> convert(
            @RequestHeader(HeaderConstants.USER_ID) UUID requesterId,
            @RequestHeader(HeaderConstants.USER_ROLE) String requesterRole,
            @RequestHeader(value = HeaderConstants.TRACE_ID, required = false) String traceId,
            @Valid @RequestBody TimeDealConvertRequest timeDealConvertRequest
    ) {
        TimeDealCreateResult timeDealCreateResult =
                timeDealCommandUseCase.convert(timeDealConvertRequest.toCommand(requesterId, requesterRole));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(TimeDealCreateResponse.from(timeDealCreateResult), traceId));
    }
}
