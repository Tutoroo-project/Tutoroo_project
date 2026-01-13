package com.tutoroo.exception;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [기능: 표준 에러 응답 객체]
 * 설명: 모든 API 에러는 이 객체 구조로 응답되어 프론트엔드에서 일관되게 처리할 수 있습니다.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String code,
        String message,
        List<FieldError> errors
) {
    /**
     * [기능: 필드 레벨의 에러 기록]
     * 작동원리: @Valid 검증 실패 시 어느 필드에서 어떤 이유로 에러가 났는지 상세 정보를 담습니다.
     */
    public record FieldError(
            String field,
            String value,
            String reason
    ) {}

    // 팩토리 메서드: 일반 예외용
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
                LocalDateTime.now(),
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );
    }
}