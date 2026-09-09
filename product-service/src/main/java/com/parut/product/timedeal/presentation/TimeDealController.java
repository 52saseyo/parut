package com.parut.product.timedeal.presentation;

import com.parut.product.global.common.ApiResponse;
import com.parut.product.global.constant.HeaderConstants;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateResult;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealCommandUseCase;
import com.parut.product.timedeal.presentation.dto.timedeal.request.TimeDealCreateRequest;
import com.parut.product.timedeal.presentation.dto.timedeal.response.TimeDealCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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


    // NOTE: 판매자 직접 등록. 일반 상품에서 전환하는 경로는 별도 엔드포인트로 붙는다.
    @PostMapping
    public ResponseEntity<ApiResponse<TimeDealCreateResponse>> create(
            @RequestHeader(HeaderConstants.USER_ID) UUID sellerId,
            @Valid @RequestBody TimeDealCreateRequest timeDealCreateRequest
    ) {
        TimeDealCreateResult timeDealCreateResult =
                timeDealCommandUseCase.create(timeDealCreateRequest.toCommand(sellerId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(TimeDealCreateResponse.from(timeDealCreateResult), null));
    }
}
