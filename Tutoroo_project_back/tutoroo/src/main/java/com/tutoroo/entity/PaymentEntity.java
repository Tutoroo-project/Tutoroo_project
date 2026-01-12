package com.tutoroo.entity;

import lombok.*;
import java.time.LocalDateTime;

/**
 * [기능: 결제 내역 엔티티]
 * 설명: DB의 payments 테이블과 매핑되어 결제 이력을 기록합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEntity {
    private Long id;
    private Long userId;
    private Long planId;        // 학습 플랜 결제인 경우 ID, 멤버십 구독이면 null 가능

    private String impUid;      // 포트원 고유번호
    private String merchantUid; // 주문번호

    // [추가됨] 결제 상품명 (예: "STANDARD SUBSCRIPTION")
    private String itemName;

    private String payMethod;   // 결제 수단 (card, trans...)
    private String pgProvider;  // PG사 정보

    private Integer amount;     // 결제 금액
    private String status;      // 결제 상태 (PAID, CANCELLED)

    private LocalDateTime paidAt; // 결제 일시
}