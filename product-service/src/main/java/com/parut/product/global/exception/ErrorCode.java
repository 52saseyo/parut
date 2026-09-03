package com.parut.product.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // auth & user
//    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 올바르지 않습니다."), // NOTE: 예시 남겨둠 시작시 삭제

    // time deal
    TIME_DEAL_INVALID_PERIOD(HttpStatus.BAD_REQUEST, "타임딜 종료일시는 시작일시보다 이후여야 합니다."),
    TIME_DEAL_INVALID_MAX_PURCHASE_QUANTITY(HttpStatus.BAD_REQUEST, "최대 구매 수량은 1개 이상이어야 합니다."),
    TIME_DEAL_NEGATIVE_STOCK_QUANTITY(HttpStatus.BAD_REQUEST, "재고 수량은 0개 이상이어야 합니다."),
    TIME_DEAL_INVALID_PURCHASE_QUANTITY(HttpStatus.BAD_REQUEST, "구매 수량은 1개 이상이어야 합니다."),
    TIME_DEAL_EXCEEDS_MAX_PURCHASE_QUANTITY(HttpStatus.BAD_REQUEST, "타임딜 최대 구매 수량을 초과했습니다."),
    TIME_DEAL_INVALID_RESERVATION_EXPIRATION(HttpStatus.BAD_REQUEST, "선점 만료 시각은 선점 시각보다 이후여야 합니다."),
    TIME_DEAL_INVALID_LOW_STOCK_THRESHOLD(HttpStatus.BAD_REQUEST, "재고 부족 임계 수량은 0개 이상이어야 합니다.")

    ;

    private final String message;
    private final HttpStatus status;

    ErrorCode(
            HttpStatus status,
            String message
    ) {
        this.status = status;
        this.message = message;

    }
}
