package com.parut.product.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException e
    ) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn(
                "[BusinessException] code={}, message={}",
                errorCode.name(),
                errorCode.getMessage()
        );

        return createResponse(errorCode);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        log.warn(
                "[MethodArgumentNotValidException] message={}",
                e.getMessage()
        );

        return createResponse(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException e
    ) {
        log.warn(
                "[BindException] message={}",
                e.getMessage()
        );

        return createResponse(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e
    ) {
        log.warn(
                "[MissingServletRequestParameterException] parameter={}",
                e.getParameterName()
        );

        return createResponse(ErrorCode.INVALID_INPUT_VALUE);
    }

    // NOTE: 별도로 두지 않으면 Exception 핸들러로 떨어져 500이 나간다.
    // 상태 코드가 401이 아니라 400인 이유: 인증은 게이트웨이가 JWT를 검증해 X-User-Id로 내려주는 구조이므로,
    // 이 헤더가 비어 있다는 것은 사용자의 인증 실패가 아니라 호출자가 헤더 전파를 빠뜨린 계약 위반이다.
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(
            MissingRequestHeaderException e
    ) {
        log.warn(
                "[MissingRequestHeaderException] header={}",
                e.getHeaderName()
        );

        return createResponse(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestPartException(
            MissingServletRequestPartException e
    ) {
        log.warn(
                "[MissingServletRequestPartException] part={}",
                e.getRequestPartName()
        );

        return createResponse(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e
    ) {
        log.warn(
                "[MethodArgumentTypeMismatchException] name={}, value={}",
                e.getName(),
                e.getValue()
        );

        return createResponse(ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e
    ) {
        log.warn(
                "[HttpMessageNotReadableException] message={}",
                e.getMessage()
        );

        return createResponse(ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e
    ) {
        log.warn(
                "[HttpMediaTypeNotSupportedException] message={}",
                e.getMessage()
        );

        return createResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e
    ) {
        log.warn(
                "[IllegalArgumentException] message={}",
                e.getMessage()
        );

        return createResponse(ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e
    ) {
        log.error(
                "[UnhandledException] type={}, message={}",
                e.getClass().getSimpleName(),
                e.getMessage(),
                e
        );

        return createResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> createResponse(
            ErrorCode errorCode
    ) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(
                        ErrorResponse.of(errorCode, null) // TODO tracing 연동 후 traceId 전달
                );
    }
}