package com.tutoroo.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * [기능: 전역 예외 핸들러]
 * 설명: 애플리케이션의 모든 에러를 포착하여 일관된 JSON 응답으로 변환합니다.
 * 작동원리:
 * 1. 커스텀 예외(TutorooException) 처리
 * 2. Bean Validation(@Valid) 실패 처리
 * 3. 그 외 예기치 못한 모든 예외 처리
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * [비즈니스 로직 예외 처리]
     */
    @ExceptionHandler(TutorooException.class)
    protected ResponseEntity<ErrorResponse> handleTutorooException(TutorooException e) {
        log.error("TutorooException: {}", e.getErrorCode().getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return new ResponseEntity<>(ErrorResponse.of(errorCode), errorCode.getStatus());
    }

    /**
     * [@Valid 검증 실패 예외 처리]
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("Validation Error: {}", e.getMessage());
        return new ResponseEntity<>(
                ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE),
                ErrorCode.INVALID_INPUT_VALUE.getStatus()
        );
    }

    /**
     * [그 외 모든 서버 내부 예외 처리]
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled Exception: ", e);
        return new ResponseEntity<>(
                ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR),
                ErrorCode.INTERNAL_SERVER_ERROR.getStatus()
        );
    }
}