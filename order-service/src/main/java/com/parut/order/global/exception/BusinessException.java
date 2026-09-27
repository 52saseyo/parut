package com.parut.order.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String code;
    private final HttpStatus status;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.code = errorCode.name();
        this.status = errorCode.getStatus();
    }

    // Feign 에서 내려준 code, message를 그대로 전달할 때 사용
    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.errorCode = null;
        this.code = code;
        this.status = status;
    }
}
