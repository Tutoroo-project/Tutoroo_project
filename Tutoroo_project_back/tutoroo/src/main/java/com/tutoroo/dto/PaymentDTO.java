package com.tutoroo.dto;

import lombok.Builder;

/**
 * [기능: 결제 데이터 전송 객체 (Record 변환 완료)]
 * 설명: 프론트엔드에서 전달받은 결제 정보와 서버의 검증 응답을 정의합니다.
 */
public class PaymentDTO {

    // 1. 결제 검증 요청
    @Builder
    public record VerificationRequest(
            String impUid,      // 포트원 결제 고유번호
            String merchantUid, // 가맹점 주문번호

            // 구독 결제인 경우 planId는 null일 수 있음 (Wrapper Class 사용)
            Long planId,

            int amount,         // 결제 금액 (9900, 29900 등)
            String payMethod,   // card, trans 등
            String pgProvider   // html5_inicis 등
    ) {}

    // 2. 결제 검증 응답
    @Builder
    public record VerificationResponse(
            boolean success,    // 성공 여부
            String message,     // 결과 메시지
            String paidAt,      // 결제 완료 시간 (ISO String)
            String nextPaymentDate // 다음 결제일 안내
    ) {}
}