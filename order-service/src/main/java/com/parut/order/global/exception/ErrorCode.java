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
    INTERNAL_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "내부 서비스 인증에 실패했습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "연동 서비스와 통신할 수 없습니다."),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "동시 요청으로 처리할 수 없습니다. 다시 시도해주세요."),
    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "현재 상태에서는 처리할 수 없습니다."),

    // order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 주문만 조회할 수 있습니다."),
    DUPLICATE_ORDER_REQUEST(HttpStatus.CONFLICT, "이미 처리 중인 주문 요청입니다."),
    PRODUCT_UNAVAILABLE(HttpStatus.CONFLICT, "판매 중이 아닌 상품입니다."),
    STOCK_SHORTAGE(HttpStatus.CONFLICT, "재고가 부족합니다."),
    ORDER_DELIVERY_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "배송 그룹을 찾을 수 없습니다."),
    ORDER_DELIVERY_GROUP_INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "배송 그룹 상태를 변경할 수 없습니다."),
    INVALID_ORDER_STATUS(HttpStatus.CONFLICT, "재고 예약이 완료되지 않은 주문입니다."),
    ORDER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "주문상품을 찾을 수 없습니다."),
    ORDER_ITEM_CONFIRMATION_NOT_ALLOWED(HttpStatus.CONFLICT, "현재 주문상품은 구매 확정할 수 없습니다."),

    // payment
    PAYMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "진행 중이거나 완료된 결제가 있습니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제를 찾을 수 없습니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.CONFLICT, "현재 결제 상태에서는 처리할 수 없습니다."),
    PG_APPROVE_FAILED(HttpStatus.BAD_GATEWAY, "PG 승인에 실패했습니다."),
    PG_CANCEL_FAILED(HttpStatus.BAD_GATEWAY, "PG 취소에 실패했습니다."),

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
