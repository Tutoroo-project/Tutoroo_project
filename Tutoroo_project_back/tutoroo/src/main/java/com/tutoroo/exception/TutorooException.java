package com.tutoroo.exception;

import lombok.Getter;

/**
 * [기능: 프로젝트 통합 예외 클래스]
 * 설명: 비즈니스 로직 실행 중 발생하는 예외를 ErrorCode와 함께 던집니다.
 * 작동원리: GlobalExceptionHandler에서 이 예외를 가로채어 규격화된 응답으로 변환합니다.
 */
@Getter
public class TutorooException extends RuntimeException {

    private final ErrorCode errorCode;

    public TutorooException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public TutorooException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}