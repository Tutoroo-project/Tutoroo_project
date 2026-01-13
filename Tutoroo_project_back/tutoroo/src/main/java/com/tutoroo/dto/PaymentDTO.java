package com.tutoroo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * [기능: 결제 데이터 전송 객체]
 * 설명: 프론트엔드에서 전달받은 결제 정보와 서버의 검증 응답을 정의합니다.
 */
public class PaymentDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerificationRequest {
        private String impUid;      // 포트원 결제 고유번호
        private String merchantUid; // 가맹점 주문번호

        // 구독 결제인 경우 planId는 null일 수 있음
        private Long planId;
        private int amount;         // 결제 금액 (9900, 29900 등)

        private String payMethod;   // card, trans 등
        private String pgProvider;  // html5_inicis 등
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerificationResponse {
        private boolean success;    // 성공 여부
        private String message;     // 결과 메시지
        private String paidAt;      // 결제 완료 시간 (ISO String)
        private String nextPaymentDate; // 다음 결제일 안내
    }
}