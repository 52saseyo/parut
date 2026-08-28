package com.parut.order.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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