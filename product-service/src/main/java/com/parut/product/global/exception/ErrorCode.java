package com.parut.product.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    // auth & user
//    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 올바르지 않습니다."), // NOTE: 예시 남겨둠 시작시 삭제

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
