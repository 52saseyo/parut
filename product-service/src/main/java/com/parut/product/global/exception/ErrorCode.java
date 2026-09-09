package com.parut.product.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "허용되지 않은 페이지 크기입니다."),
    INVALID_SORT_FIELD(HttpStatus.BAD_REQUEST, "허용되지 않은 정렬 기준입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // internal auth
    // 호출자 서비스 자체의 인증 실패이므로 401이다. X-User-Id 헤더 누락(400)과 구분할 것 —
    // 그쪽은 게이트웨이가 인증을 끝낸 뒤 호출자가 헤더 전파를 빠뜨린 계약 위반이다.
    INTERNAL_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "내부 서비스 인증에 실패했습니다."),

    // product_stock
    PRODUCT_STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "재고 정보가 없습니다."),
    PRODUCT_STOCK_SHORTAGE(HttpStatus.CONFLICT, "요청 수량이 재고를 초과합니다."),
    PRODUCT_STOCK_PRODUCT_NOT_ON_SALE(HttpStatus.CONFLICT, "판매 중이 아닌 상품입니다."),
    PRODUCT_STOCK_INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "재고 수량이 유효하지 않습니다."),
    PRODUCT_STOCK_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 상품에 대한 권한이 없습니다."),
    PRODUCT_STOCK_CONFLICT(HttpStatus.CONFLICT, "다른 요청과 충돌하여 처리하지 못했습니다. 다시 시도해주세요."),
    PRODUCT_STOCK_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 재고입니다."),
    PRODUCT_STOCK_DELETE_NOT_ALLOWED(HttpStatus.CONFLICT, "예약 중인 재고가 있어 삭제할 수 없습니다."),
    PRODUCT_STOCK_RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 예약 건이 없습니다."),
    PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 예약입니다."),
    PRODUCT_STOCK_RESERVATION_EXPIRED(HttpStatus.CONFLICT, "예약이 만료되어 처리할 수 없습니다."),
    PRODUCT_STOCK_RESERVATION_ISOLATED(HttpStatus.CONFLICT, "재고 복구 여부가 확인되지 않아 격리된 예약입니다. 관리자 확인이 필요합니다."),
    PRODUCT_STOCK_PAGE_INVALID_SIZE(HttpStatus.BAD_REQUEST, "허용되지 않은 페이지 크기입니다."),
    PRODUCT_STOCK_SORT_INVALID_FIELD(HttpStatus.BAD_REQUEST, "허용되지 않은 정렬 기준입니다."),

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
    TIME_DEAL_STOCK_DELETE_NOT_ALLOWED(HttpStatus.CONFLICT, "선점 또는 판매된 수량이 있는 재고는 삭제할 수 없습니다."),
    TIME_DEAL_DELETED(HttpStatus.CONFLICT, "삭제된 타임딜입니다."),
    TIME_DEAL_STOCK_DELETED(HttpStatus.CONFLICT, "삭제된 타임딜 재고입니다."),
    TIME_DEAL_INVALID_CANCEL_REASON(HttpStatus.BAD_REQUEST, "취소 사유는 30자를 넘을 수 없습니다."),
    TIME_DEAL_PURCHASE_DELETE_NOT_ALLOWED(HttpStatus.CONFLICT, "타임딜 구매 이력은 삭제할 수 없습니다."),
    TIME_DEAL_INVALID_STATUS(HttpStatus.CONFLICT, "삭제할 수 없는 타임딜 상태입니다."),
    TIME_DEAL_UPDATE_NOT_ALLOWED(HttpStatus.CONFLICT, "수정할 수 없는 타임딜 상태입니다."),
    TIME_DEAL_INVALID_PRICE(HttpStatus.BAD_REQUEST, "타임딜 판매 가격은 0보다 커야 합니다."),
    TIME_DEAL_STOCK_INSUFFICIENT(HttpStatus.CONFLICT, "판매 가능한 재고가 부족합니다."),
    TIME_DEAL_SALE_PERIOD_INVALID(HttpStatus.CONFLICT, "현재 시각이 타임딜 판매 기간이 아닙니다."),
    TIME_DEAL_INVALID_STOCK_QUANTITY(HttpStatus.BAD_REQUEST, "초기 재고는 1개 이상이어야 합니다."),
    TIME_DEAL_INVALID_STOCK_ADJUST_QUANTITY(HttpStatus.BAD_REQUEST, "재고 조정 수량은 0일 수 없습니다."),
    TIME_DEAL_STOCK_ADJUST_NOT_ALLOWED(HttpStatus.CONFLICT, "종료되거나 중단된 타임딜의 재고는 조정할 수 없습니다."),
    TIME_DEAL_MAX_PURCHASE_QUANTITY_EXCEEDS_STOCK(HttpStatus.BAD_REQUEST, "1인당 최대 구매 수량은 초기 재고 수량을 넘을 수 없습니다."),
    TIME_DEAL_STOCK_MISMATCH(HttpStatus.CONFLICT, "다른 타임딜의 재고 또는 구매 이력입니다."),
    TIME_DEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "타임딜을 찾을 수 없습니다."),
    TIME_DEAL_STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "타임딜 재고 정보가 없습니다."),
    TIME_DEAL_PURCHASE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 처리된 주문입니다."),
    TIME_DEAL_PURCHASE_NOT_FOUND(HttpStatus.NOT_FOUND, "타임딜 구매 이력을 찾을 수 없습니다."),
    TIME_DEAL_SALE_PERIOD_NOT_ENDED(HttpStatus.CONFLICT, "판매 기간이 끝나지 않은 타임딜입니다."),
    TIME_DEAL_INVALID_DISCOUNT_RATE(HttpStatus.BAD_REQUEST, "할인율은 0 이상 100 미만이어야 합니다."),


    // product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    PRODUCT_NOT_MODIFIABLE(HttpStatus.CONFLICT, "현재 상태에서는 상품을 수정할 수 없습니다."),
    PRODUCT_ALREADY_DELETED(HttpStatus.CONFLICT, "이미 삭제된 상품입니다."),
    PRODUCT_STATUS_TRANSITION_NOT_ALLOWED(HttpStatus.CONFLICT, "허용되지 않은 상품 상태 변경입니다."),
    PRODUCT_MAIN_IMAGE_REQUIRED(HttpStatus.CONFLICT, "대표 이미지가 등록된 상품만 판매할 수 있습니다."),
    PRODUCT_MAIN_IMAGE_ALREADY_EXISTS(HttpStatus.CONFLICT, "대표 이미지는 하나만 등록할 수 있습니다."),
    PRODUCT_INVALID_SELLER_ID(HttpStatus.BAD_REQUEST, "판매자 ID는 필수입니다."),
    PRODUCT_INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "상품 카테고리는 필수입니다."),
    PRODUCT_INVALID_NAME(HttpStatus.BAD_REQUEST, "상품명이 올바르지 않습니다."),
    PRODUCT_INVALID_PRICE(HttpStatus.BAD_REQUEST, "상품 가격은 0 이상이어야 합니다."),
    PRODUCT_INVALID_APPEARANCE_TYPE(HttpStatus.BAD_REQUEST, "상품 외관 유형은 필수입니다."),
    PRODUCT_INVALID_ORIGIN(HttpStatus.BAD_REQUEST, "원산지가 올바르지 않습니다."),
    PRODUCT_INVALID_HARVEST_DATE(HttpStatus.BAD_REQUEST, "수확일은 필수입니다."),
    PRODUCT_INVALID_SALE_UNIT(HttpStatus.BAD_REQUEST, "판매 단위는 필수입니다."),
    PRODUCT_INVALID_UNIT_QUANTITY(HttpStatus.BAD_REQUEST, "판매 단위 수량은 0보다 커야 합니다."),
    PRODUCT_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "상품 이미지를 찾을 수 없습니다."),
    PRODUCT_IMAGE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 상품 이미지입니다."),
    PRODUCT_IMAGE_PRODUCT_REQUIRED(HttpStatus.BAD_REQUEST, "상품은 필수입니다."),
    PRODUCT_IMAGE_INVALID_KEY(HttpStatus.BAD_REQUEST, "이미지 키가 올바르지 않습니다."),
    PRODUCT_IMAGE_INVALID_TYPE(HttpStatus.BAD_REQUEST, "이미지 타입은 필수입니다."),
    PRODUCT_IMAGE_INVALID_SORT_ORDER(HttpStatus.BAD_REQUEST, "이미지 노출 순서는 0 이상이어야 합니다."),
    PRODUCT_DETAIL_IMAGE_SORT_ORDER_INVALID(HttpStatus.BAD_REQUEST, "상세 이미지의 노출 순서는 1 이상이어야 합니다."),
    PRODUCT_IMAGE_NOT_MODIFIABLE(HttpStatus.CONFLICT, "현재 상품 상태에서는 이미지를 변경할 수 없습니다."),
    PRODUCT_DETAIL_IMAGE_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "상품 상세 이미지는 최대 5장까지 등록할 수 있습니다."),
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
