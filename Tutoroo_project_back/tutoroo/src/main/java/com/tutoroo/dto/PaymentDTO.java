package com.tutoroo.dto;

import lombok.Builder;
import java.util.List;

/**
 * [기능: 결제 데이터 전송 객체 (Record)]
 * 설명: 결제 검증 요청과 결제 내역 조회 응답을 포함합니다.
 */
public class PaymentDTO {

    // 1. 결제 검증 요청
    // [수정] itemName 필드 추가 (PaymentService 에러 해결용)
    @Builder
    public record VerificationRequest(
            String impUid,      // 포트원 결제 고유번호 (필수)
            String merchantUid, // 가맹점 주문번호 (필수)
            String planId,        // 학습 플랜 ID (구독이면 null)
            int amount,         // 결제 금액 (위변조 검증용)
            String itemName,    // [New] 상품명 (예: PREMIUM_SUBSCRIPTION) - 필수!
            String payMethod,   // 결제 수단 (card, trans 등)
            String pgProvider   // PG사 정보 (html5_inicis 등)
    ) {}

    // 2. 결제 검증 응답
    @Builder
    public record VerificationResponse(
            boolean success,    // 성공 여부
            String message,     // 결과 메시지
            String paidAt,      // 결제 완료 시간
            String nextPaymentDate // 다음 결제일 안내 (구독형)
    ) {}

    // 3. 결제 내역 목록 응답 (마이페이지용)
    @Builder
    public record HistoryResponse(
            List<PaymentInfo> payments, // 결제 리스트
            long totalSpent             // 총 누적 결제액
    ) {}

    // 4. 개별 결제 정보 (HistoryResponse 내부용)
    @Builder
    public record PaymentInfo(
            String merchantUid, // 주문번호
            String itemName,    // 상품명
            int amount,         // 금액
            String payMethod,   // 결제 수단
            String status,      // 결제 상태 (PAID, CANCELLED)
            String paidAt       // 결제 일시 (YYYY-MM-DD HH:mm)
    ) {}

    // 5. 웹훅 요청 객체 (포트원 -> 백엔드)
    // 포트원 서버가 보내주는 JSON 키값(snake_case)과 일치해야 함
    public record WebhookRequest(
            String imp_uid,      // 포트원 고유번호
            String merchant_uid, // 주문번호
            String status        // 결제 상태 (paid, ready, failed, cancelled)
    ) {}
}