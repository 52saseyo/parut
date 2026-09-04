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

    // product_stock
    PRODUCT_STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "재고 정보가 없습니다."),
    PRODUCT_STOCK_SHORTAGE(HttpStatus.CONFLICT, "요청 수량이 재고를 초과합니다."),
    PRODUCT_STOCK_PRODUCT_NOT_ON_SALE(HttpStatus.CONFLICT, "판매 중이 아닌 상품입니다."),
    PRODUCT_STOCK_INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "재고 수량이 유효하지 않습니다."),
    PRODUCT_STOCK_CONFLICT(HttpStatus.CONFLICT, "다른 요청과 충돌하여 처리하지 못했습니다. 다시 시도해주세요."),
    PRODUCT_STOCK_RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 예약 건이 없습니다."),
    PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 예약입니다."),
    PRODUCT_STOCK_RESERVATION_EXPIRED(HttpStatus.CONFLICT, "예약이 만료되어 처리할 수 없습니다."),
    PRODUCT_STOCK_PAGE_INVALID_SIZE(HttpStatus.BAD_REQUEST, "허용되지 않은 페이지 크기입니다."),
    PRODUCT_STOCK_SORT_INVALID_FIELD(HttpStatus.BAD_REQUEST, "허용되지 않은 정렬 기준입니다.");


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
