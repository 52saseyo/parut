package com.parut.order.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // order
    ORDER_DELIVERY_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "배송 그룹을 찾을 수 없습니다."),
    ORDER_DELIVERY_GROUP_INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "배송 그룹 상태를 변경할 수 없습니다."),

    // delivery
    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "배송 정보를 찾을 수 없습니다."),
    DELIVERY_INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "배송 상태를 변경할 수 없습니다."),
    DELIVERY_NO_SHIPPABLE_ITEMS(HttpStatus.CONFLICT, "배송할 수 있는 주문상품이 없습니다."),

    // refund
    REFUND_NOT_FOUND(HttpStatus.NOT_FOUND, "환불 요청을 찾을 수 없습니다."),
    REFUND_NOT_ALLOWED(HttpStatus.CONFLICT, "환불을 요청할 수 없습니다."),
    REFUND_ALREADY_REQUESTED(HttpStatus.CONFLICT, "처리 중인 환불 요청이 이미 존재합니다."),
    REFUND_CANCEL_NOT_ALLOWED(HttpStatus.CONFLICT, "환불 요청을 취소할 수 없습니다."),
    REFUND_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 환불 요청입니다."),

    // settlement
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "정산 대상을 찾을 수 없습니다."),
    SETTLEMENT_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완료된 정산입니다."),
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
