package com.parut.product.timedeal.presentation;

import com.parut.product.global.common.ApiResponse;
import com.parut.product.global.common.CursorPageInfo;
import com.parut.product.global.common.CursorResponse;
import com.parut.product.global.common.SortDirection;
import com.parut.product.global.constant.HeaderConstants;
import com.parut.product.global.logging.TraceIdContext;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCursorResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealUpdateResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealDeleteCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealStopCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealStopResult;
import com.parut.product.timedeal.presentation.dto.timedeal.response.TimeDealUpdateResponse;
import com.parut.product.timedeal.presentation.dto.timedeal.response.TimeDealStopResponse;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealCommandUseCase;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealQueryUseCase;
import com.parut.product.timedeal.presentation.dto.timedeal.response.TimeDealPublicDetailResponse;
import com.parut.product.timedeal.presentation.support.TimeDealCursorRequestValidator;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;
import org.springframework.web.bind.annotation.GetMapping;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/time-deals")
@RequiredArgsConstructor
public class TimeDealController {

    private final TimeDealCommandUseCase timeDealCommandUseCase;
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

    @DeleteMapping("/{timeDealId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID timeDealId,
            @RequestHeader(HeaderConstants.USER_ID) UUID requesterId,
            @RequestHeader(HeaderConstants.USER_ROLE) String requesterRole
    ) {
        timeDealCommandUseCase.delete(new TimeDealDeleteCommand(timeDealId, requesterId, requesterRole));
        return ResponseEntity.ok(ApiResponse.success(null, TraceIdContext.currentTraceId()));
    }

    @PatchMapping("/{timeDealId}/stop")
    public ResponseEntity<ApiResponse<TimeDealStopResponse>> stop(
            @PathVariable UUID timeDealId,
            @RequestHeader(HeaderConstants.USER_ID) UUID requesterId,
            @RequestHeader(HeaderConstants.USER_ROLE) String requesterRole
    ) {
        TimeDealStopResult timeDealStopResult = timeDealCommandUseCase.stop(
                new TimeDealStopCommand(timeDealId, requesterId, requesterRole));
        return ResponseEntity.ok(ApiResponse.success(
                TimeDealStopResponse.from(timeDealStopResult), TraceIdContext.currentTraceId()));
    }

    @PatchMapping("/{timeDealId}")
    public ResponseEntity<ApiResponse<TimeDealUpdateResponse>> update(
            @PathVariable UUID timeDealId,
            @RequestHeader(HeaderConstants.USER_ID) UUID requesterId,
            @RequestHeader(HeaderConstants.USER_ROLE) String requesterRole,
            @Valid @RequestBody TimeDealUpdateRequest request
    ) {
        TimeDealUpdateResult timeDealUpdateResult =
                timeDealCommandUseCase.update(request.toCommand(timeDealId, requesterId, requesterRole));
        return ResponseEntity.ok(ApiResponse.success(
                TimeDealUpdateResponse.from(timeDealUpdateResult), TraceIdContext.currentTraceId()));
    }


    // NOTE: 판매자 직접 등록. 일반 상품에서 전환하는 경로는 별도 엔드포인트로 붙는다.
    @PostMapping
    public ResponseEntity<ApiResponse<TimeDealCreateResponse>> create(
            @RequestHeader(HeaderConstants.USER_ID) UUID sellerId,
            @RequestHeader(HeaderConstants.USER_ROLE) String requesterRole,
            @Valid @RequestBody TimeDealCreateRequest timeDealCreateRequest
    ) {
        TimeDealCreateResult timeDealCreateResult =
                timeDealCommandUseCase.create(timeDealCreateRequest.toCommand(sellerId, requesterRole));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        TimeDealCreateResponse.from(timeDealCreateResult), TraceIdContext.currentTraceId()));
    }


    // NOTE: 일반 상품 전환. 요청자가 판매자 본인이 아닐 수 있어(운영자) role까지 넘긴다 — 소유권 판정은 product 몫이다.
    @PostMapping("/conversions")
    public ResponseEntity<ApiResponse<TimeDealCreateResponse>> convert(
            @RequestHeader(HeaderConstants.USER_ID) UUID requesterId,
            @RequestHeader(HeaderConstants.USER_ROLE) String requesterRole,
            @Valid @RequestBody TimeDealConvertRequest timeDealConvertRequest
    ) {
        TimeDealCreateResult timeDealCreateResult =
                timeDealCommandUseCase.convert(timeDealConvertRequest.toCommand(requesterId, requesterRole));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        TimeDealCreateResponse.from(timeDealCreateResult), TraceIdContext.currentTraceId()));
    }
}
