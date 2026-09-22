package com.parut.notification.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "size는 10, 30, 50만 허용됩니다."),

    // notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    INVALID_NOTIFICATION_CURSOR(HttpStatus.BAD_REQUEST, "cursor와 cursorId는 함께 전달해야 합니다."),
    NOTIFICATION_READ_AT_REQUIRED(HttpStatus.BAD_REQUEST, "알림 읽음 시각은 필수입니다."),
    NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "알림에 접근할 권한이 없습니다."),

    //subscription
    TIME_DEAL_SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "타임딜 알림 신청 내역을 찾을 수 없습니다."),
    NOTIFICATION_SUBSCRIPTION_USER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "사용자 ID는 필수입니다."),
    NOTIFICATION_SUBSCRIPTION_TIME_DEAL_ID_REQUIRED(HttpStatus.BAD_REQUEST, "타임딜 ID는 필수입니다."),
    NOTIFICATION_SUBSCRIPTION_CANCEL_TIME_REQUIRED(HttpStatus.BAD_REQUEST, "알림 신청 취소 시각은 필수입니다."),
    TIME_DEAL_SUBSCRIPTION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "타임딜 알림 신청에 대한 권한이 없습니다."),


    // time-deal opening-soon event
    TIME_DEAL_OPENING_SOON_EVENT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "이벤트 ID는 필수입니다."),
    TIME_DEAL_OPENING_SOON_TIME_DEAL_ID_REQUIRED(HttpStatus.BAD_REQUEST, "타임딜 ID는 필수입니다."),
    TIME_DEAL_OPENING_SOON_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "타임딜 상품명은 필수입니다."),
    TIME_DEAL_OPENING_SOON_START_AT_REQUIRED(HttpStatus.BAD_REQUEST, "타임딜 시작 시각은 필수입니다."),
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
