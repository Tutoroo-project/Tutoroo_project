package com.tutoroo.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // --- [Common: 공통 에러 (C)] ---
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "올바르지 않은 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다."),
    FILE_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "파일 업로드 중 오류가 발생했습니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "C005", "요청하신 파일을 찾을 수 없습니다."),

    // --- [User: 사용자 및 인증 (U)] ---
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_ID(HttpStatus.CONFLICT, "U002", "이미 사용 중인 아이디(이메일)입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "U003", "비밀번호가 일치하지 않습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "U005", "접근 권한이 없습니다. (로그인 필요)"),
    INVALID_AUTH_CODE(HttpStatus.BAD_REQUEST, "U006", "인증번호가 일치하지 않거나 만료되었습니다."),
    PARENT_PHONE_REQUIRED(HttpStatus.BAD_REQUEST, "U007", "만 19세 미만 회원은 보호자 연락처가 필수입니다."),

    // --- [Learning: 학습 및 AI (L)] ---
    AI_PROCESSING_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "L001", "AI 응답 생성 중 오류가 발생했습니다."),
    STUDY_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "L002", "학습 계획(플랜)을 찾을 수 없습니다."),
    MULTIPLE_PLANS_REQUIRED_PAYMENT(HttpStatus.PAYMENT_REQUIRED, "L003", "추가 목표 설정은 유료 멤버십 기능입니다."),
    INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "L004", "보유 포인트가 부족합니다."),
    STT_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "L005", "음성 인식(STT) 처리 중 오류가 발생했습니다."),

    // --- [Pet: 펫/다마고치 (P)] ---
    // 아래 부분이 PetService에서 사용하는 핵심 에러들입니다.
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "육성 중인 펫 정보를 찾을 수 없습니다."),
    ALREADY_HAS_PET(HttpStatus.BAD_REQUEST, "P002", "이미 육성 중인 펫이 있습니다. 졸업 후 입양해주세요."),
    INVALID_PET_TYPE(HttpStatus.BAD_REQUEST, "P003", "유효하지 않은 펫 종류입니다."),
    MEMBERSHIP_PET_RESTRICTION(HttpStatus.FORBIDDEN, "P004", "현재 멤버십 등급에서는 선택할 수 없는 펫입니다."),
    PET_IS_SLEEPING(HttpStatus.BAD_REQUEST, "P005", "펫이 자고 있어 상호작용할 수 없습니다."),
    PET_TOO_TIRED(HttpStatus.BAD_REQUEST, "P006", "펫이 너무 피곤해합니다. 휴식이 필요합니다."),
    EGG_SELECTION_EXPIRED(HttpStatus.BAD_REQUEST, "P007", "알 선택 시간이 만료되었습니다. 다시 시도해주세요."),
    INVALID_EGG_CANDIDATE(HttpStatus.BAD_REQUEST, "P008", "제공된 알 후보 목록에 없는 펫입니다."),

    // --- [Payment: 결제 (M)] ---
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "M001", "결제 처리에 실패했습니다."),
    PAYMENT_CANCEL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "M002", "결제 취소 처리에 실패했습니다."),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "M003", "결제 금액이 일치하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}