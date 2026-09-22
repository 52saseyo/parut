package com.parut.user.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    PWD_NOT_MATCH(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),

    // S: USER
    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "사용자를 찾을 수 없습니다."
    ),

    USER_ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "사용자 관리 권한이 없습니다."
    ),

    ACTIVE_ORDER_EXISTS(
            HttpStatus.BAD_REQUEST,
            "진행 중인 주문이 있어 탈퇴할 수 없습니다."
    ),

    DUPLICATE_USERNAME(
            HttpStatus.CONFLICT,
            "이미 사용 중인 사용자명입니다."
    ),

    DUPLICATE_SLACK_ID(
            HttpStatus.CONFLICT,
            "이미 사용 중인 Slack ID입니다."
    ),

    USER_DEL_ACCOUNT(
            HttpStatus.FORBIDDEN,
            "탈퇴한 계정입니다."
    ),
    // E: USER

    // S: SELLER
    SELLER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "판매자를 찾을 수 없습니다."
    ),

    SELLER_ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "판매자 관리 권한이 없습니다."
    ),

    SELLER_EXIST_ORDERS(
            HttpStatus.FORBIDDEN,
            "진행 중인 주문이 있어 탈퇴할 수 없습니다."
    ),

    ORDER_SERVICE_UNAVAILABLE(
            HttpStatus.FORBIDDEN,
            "주문 데이터 조회 오류가 발생했습니다."
    ),

    SELLER_DEL_ACCOUNT(
            HttpStatus.FORBIDDEN,
            "탈퇴한 계정입니다."
    ),
    // E: SELLER

    // S: ADMIN
    ADMIN_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "존재하지 않는 관리자 계정입니다."
    ),
    // E: ADMIN

    // S: ADDRESS
    ADDRESS_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "배송지를 찾을 수 없습니다."
    ),

    ADDRESS_DEFAULT_CONFLICT(
            HttpStatus.CONFLICT,
            "기본 배송지가 변경 중입니다. 잠시 후 다시 시도해주세요."
    ),
    // E: ADDRESS

    // S: AUTH
    INVALID_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "유효하지 않거나 만료된 토큰입니다."
    ),

    INVALID_REFRESH_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "유효하지 않거나 만료된 리프레시 토큰입니다. 다시 로그인해주세요."
    );
    // E: AUTH

    private final HttpStatus status;
    private final String message;

}
