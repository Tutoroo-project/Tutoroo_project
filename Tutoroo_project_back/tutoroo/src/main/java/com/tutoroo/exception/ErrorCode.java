package com.tutoroo.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "올바르지 않은 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다."),

    // Auth & User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_ID(HttpStatus.CONFLICT, "U002", "이미 사용 중인 아이디입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "U003", "비밀번호가 일치하지 않습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "U005", "로그인이 필요한 서비스입니다."),

    // AI & Learning
    AI_PROCESSING_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "L001", "AI 응답 생성 중 오류가 발생했습니다."),
    STUDY_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "L002", "학습 계획을 찾을 수 없습니다."),
    MULTIPLE_PLANS_REQUIRED_PAYMENT(HttpStatus.PAYMENT_REQUIRED, "L003", "두 개 이상의 목표는 결제가 필요합니다."),
    INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "L004", "포인트가 부족하여 기능을 사용할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}