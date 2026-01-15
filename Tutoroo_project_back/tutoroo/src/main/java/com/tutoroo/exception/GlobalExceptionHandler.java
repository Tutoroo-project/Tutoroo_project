package com.tutoroo.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TutorooException.class)
    protected ResponseEntity<ErrorResponse> handleTutorooException(TutorooException e) {
        log.warn("TutorooException: code={}, message={}", e.getErrorCode().getCode(), e.getErrorCode().getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return new ResponseEntity<>(ErrorResponse.of(errorCode), errorCode.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Validation Error: {}", e.getMessage());
        return new ResponseEntity<>(
                ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE),
                ErrorCode.INVALID_INPUT_VALUE.getStatus()
        );
    }

    // 권한이 없는 접근 (예: 로그인 안 된 상태로 요청하거나, 본인 데이터가 아닌 것에 접근 시)
    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access Denied: {}", e.getMessage());
        return new ResponseEntity<>(
                ErrorResponse.of(ErrorCode.UNAUTHORIZED_ACCESS),
                ErrorCode.UNAUTHORIZED_ACCESS.getStatus()
        );
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected Exception: ", e);
        return new ResponseEntity<>(
                ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR),
                ErrorCode.INTERNAL_SERVER_ERROR.getStatus()
        );
    }
}