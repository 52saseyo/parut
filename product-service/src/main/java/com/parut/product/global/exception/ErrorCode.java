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
    TIME_DEAL_EXCEEDS_MAX_PURCHASE_QUANTITY(HttpStatus.CONFLICT, "타임딜 최대 구매 수량을 초과했습니다."),
    TIME_DEAL_INVALID_LOW_STOCK_THRESHOLD(HttpStatus.BAD_REQUEST, "재고 부족 임계 수량은 0개 이상이며 초기 재고 이하이어야 합니다."),
    TIME_DEAL_INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "허용되지 않는 타임딜 상태 전이입니다."),
    TIME_DEAL_NOT_ACTIVE(HttpStatus.CONFLICT, "타임딜이 판매 중 상태가 아닙니다."),
    TIME_DEAL_RESERVATION_EXPIRED(HttpStatus.CONFLICT, "선점 시간이 만료되어 판매를 확정할 수 없습니다."),
    TIME_DEAL_ACTIVE_DELETE_NOT_ALLOWED(HttpStatus.CONFLICT, "판매 중인 타임딜은 삭제할 수 없습니다."),
    TIME_DEAL_INVALID_STATUS(HttpStatus.CONFLICT, "삭제할 수 없는 타임딜 상태입니다."),
    TIME_DEAL_UPDATE_NOT_ALLOWED(HttpStatus.CONFLICT, "수정할 수 없는 타임딜 상태입니다."),
    TIME_DEAL_INVALID_PRICE(HttpStatus.BAD_REQUEST, "타임딜 판매 가격은 0보다 커야 합니다."),
    TIME_DEAL_STOCK_INSUFFICIENT(HttpStatus.CONFLICT, "판매 가능한 재고가 부족합니다."),
    TIME_DEAL_SALE_PERIOD_INVALID(HttpStatus.CONFLICT, "현재 시각이 타임딜 판매 기간이 아닙니다."),
    TIME_DEAL_INVALID_STOCK_QUANTITY(HttpStatus.BAD_REQUEST, "초기 재고는 1개 이상이어야 합니다."),
    TIME_DEAL_INVALID_STOCK_ADJUST_QUANTITY(HttpStatus.BAD_REQUEST, "재고 조정 수량은 0일 수 없습니다."),
    TIME_DEAL_INVALID_DISCOUNT_RATE(HttpStatus.BAD_REQUEST, "할인율은 0 이상 100 미만이어야 합니다.")

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
