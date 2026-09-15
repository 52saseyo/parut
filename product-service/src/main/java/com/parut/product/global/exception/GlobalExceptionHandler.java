package com.parut.product.global.exception;

import lombok.extern.slf4j.Slf4j;
import com.parut.product.global.constant.HeaderConstants;
import jakarta.servlet.http.HttpServletRequest;
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
            BusinessException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn(
                "[BusinessException] code={}, message={}",
                errorCode.name(),
                errorCode.getMessage()
        );

        return createResponse(errorCode, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        log.warn(
                "[MethodArgumentNotValidException] message={}",
                e.getMessage()
        );

        return createResponse(ErrorCode.INVALID_INPUT_VALUE, request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException e,
            HttpServletRequest request
    ) {
        log.warn(
                "[BindException] message={}",
                e.getMessage()
        );

        return createResponse(ErrorCode.INVALID_INPUT_VALUE, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e,
            HttpServletRequest request
    ) {
        log.warn(
                "[MissingServletRequestParameterException] parameter={}",
                e.getParameterName()
        );

        return createResponse(ErrorCode.INVALID_INPUT_VALUE, request);
    }

    // NOTE: 필수 헤더 값이 빠졌을때 예외. 인증은 게이트웨이가 JWT를 검증해 X-User-Id로 내려주는 구조이므로, 헤더가 비어 있다는 것은 사용자의 인증 실패가 아니라 호출자가 헤더 전파를 빠뜨린 계약 위반.
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(
            MissingRequestHeaderException e,
            HttpServletRequest request
    ) {
        log.warn(
                "[MissingRequestHeaderException] header={}",
                e.getHeaderName()
        );

        return createResponse(ErrorCode.INVALID_INPUT_VALUE, request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestPartException(
            MissingServletRequestPartException e,
            HttpServletRequest request
    ) {
        log.warn(
                "[MissingServletRequestPartException] part={}",
                e.getRequestPartName()
        );

        return createResponse(ErrorCode.INVALID_INPUT_VALUE, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request
    ) {
        log.warn(
                "[MethodArgumentTypeMismatchException] name={}, value={}",
                e.getName(),
                e.getValue()
        );

        return createResponse(ErrorCode.INVALID_REQUEST, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        log.warn(
                "[HttpMessageNotReadableException] message={}",
                e.getMessage()
        );

        return createResponse(ErrorCode.INVALID_REQUEST, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e,
            HttpServletRequest request
    ) {
        log.warn(
                "[HttpMediaTypeNotSupportedException] message={}",
                e.getMessage()
        );

        return createResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e,
            HttpServletRequest request
    ) {
        log.warn(
                "[IllegalArgumentException] message={}",
                e.getMessage()
        );

        return createResponse(ErrorCode.INVALID_REQUEST, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e,
            HttpServletRequest request
    ) {
        log.error(
                "[UnhandledException] type={}, message={}",
                e.getClass().getSimpleName(),
                e.getMessage(),
                e
        );

        return createResponse(ErrorCode.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<ErrorResponse> createResponse(
            ErrorCode errorCode,
            HttpServletRequest request
    ) {
        String traceId = request.getHeader(HeaderConstants.TRACE_ID);
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(
                        ErrorResponse.of(errorCode, traceId)
                );
    }
}
