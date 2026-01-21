package com.tutoroo.service;

import com.tutoroo.dto.PaymentDTO;
import com.tutoroo.entity.MembershipTier;
import com.tutoroo.entity.PaymentEntity;
import com.tutoroo.entity.UserEntity;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.PaymentMapper;
import com.tutoroo.mapper.UserMapper;
import com.tutoroo.util.PortOneClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserMapper userMapper;
    private final PaymentMapper paymentMapper;
    private final PortOneClient portOneClient;

    /**
     * [기능: 결제 검증 및 멤버십 업그레이드 (보안 강화판)]
     * 변경점: 클라이언트 요청 금액(request.amount)을 신뢰하지 않고,
     * PG사 실제 결제 내역조회 + 서버 정가 비교를 수행합니다.
     */
    @Transactional
    public PaymentDTO.VerificationResponse verifyAndUpgrade(PaymentDTO.VerificationRequest request, String username) {
        try {
            // 1. 사용자 확인
            UserEntity user = userMapper.findByUsername(username);
            if (user == null) {
                throw new TutorooException(ErrorCode.USER_NOT_FOUND);
            }

            // 2. [보안 핵심] PortOne 단건 조회를 통해 '실제' 결제 정보 가져오기
            // 프론트엔드에서 보낸 데이터는 조작될 수 있으므로 무시하고 imp_uid로 직접 조회합니다.
            Map<String, Object> paymentData = portOneClient.getPayment(request.impUid());

            if (paymentData == null) {
                throw new TutorooException("유효하지 않은 결제 건입니다.", ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }

            String status = (String) paymentData.get("status");
            Integer paidAmount = (Integer) paymentData.get("amount");
            String paidMerchantUid = (String) paymentData.get("merchant_uid");
            String pgProvider = (String) paymentData.get("pg_provider");
            String payMethod = (String) paymentData.get("pay_method");

            // 3. 결제 상태 확인
            if (!"paid".equals(status)) {
                throw new TutorooException("결제가 완료되지 않았습니다. 현재 상태: " + status, ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }

            // 4. [보안 핵심] 결제 금액 변조 검증 (서버 정가 vs 실제 결제 금액)
            // 요청된 상품명(itemName)에 따른 정확한 가격을 서버에서 가져옵니다.
            String itemName = request.itemName(); // 예: "PREMIUM_SUBSCRIPTION"
            int requiredAmount = getPriceByItemName(itemName);

            if (paidAmount == null || paidAmount != requiredAmount) {
                log.warn("🚨 결제 금액 불일치 감지! (User: {}, 요청: {}, 실결제: {}) -> 자동 환불 처리",
                        username, requiredAmount, paidAmount);

                // 금액이 다르면 해킹 시도로 간주하고 즉시 결제 취소(환불)
                portOneClient.cancelPayment(request.impUid(), "결제 금액 위변조 감지 (System Auto Refund)");

                throw new TutorooException("결제 금액이 올바르지 않습니다.", ErrorCode.INVALID_PAYMENT_AMOUNT);
            }

            // 5. 멤버십 등급 결정
            MembershipTier newTier = getTierByItemName(itemName);

            if (user.getEffectiveTier() == newTier) {
                log.info("ℹ️ 기존과 동일한 등급 결제입니다. (연장 처리 등): {}", username);
            }

            // 6. DB 반영 (멤버십 등급 업데이트)
            // UserMapper.xml의 COALESCE 적용으로 안전하게 업데이트됨
            user.setMembershipTier(newTier);
            userMapper.update(user);

            // 7. 결제 내역 저장
            PaymentEntity payment = PaymentEntity.builder()
                    .userId(user.getId())
                    .planId(request.planId()) // 멤버십 구독인 경우 null
                    .impUid(request.impUid())
                    .merchantUid(paidMerchantUid)
                    .itemName(itemName)
                    .amount(paidAmount)
                    .payMethod(payMethod)
                    .pgProvider(pgProvider)
                    .status("PAID")
                    .paidAt(LocalDateTime.now())
                    .build();

            paymentMapper.save(payment);

            log.info("✅ [결제 성공] User: {}, Amount: {}, Tier Upgraded to: {}", username, paidAmount, newTier);

            return PaymentDTO.VerificationResponse.builder()
                    .success(true)
                    .message(String.format("멤버십이 %s 등급으로 업그레이드 되었습니다.", newTier.name()))
                    .paidAt(LocalDateTime.now().toString())
                    .nextPaymentDate(LocalDateTime.now().plusMonths(1).toString()) // 구독형 가정
                    .build();

        } catch (TutorooException te) {
            throw te; // 비즈니스 로직 예외는 그대로 던짐
        } catch (Exception e) {
            log.error("❌ 시스템 오류로 인한 결제 검증 실패. impUid={}", request.impUid(), e);
            throw new TutorooException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * [기능: 내 결제 내역 조회]
     */
    @Transactional(readOnly = true)
    public PaymentDTO.HistoryResponse getPaymentHistory(Long userId) {
        // 1. DB 조회
        List<PaymentEntity> payments = paymentMapper.findAllByUserId(userId);

        // 2. DTO 변환
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<PaymentDTO.PaymentInfo> paymentInfos = payments.stream()
                .map(p -> PaymentDTO.PaymentInfo.builder()
                        .merchantUid(p.getMerchantUid())
                        .itemName(p.getItemName())
                        .amount(p.getAmount())
                        .payMethod(p.getPayMethod())
                        .status(p.getStatus())
                        .paidAt(p.getPaidAt().format(formatter))
                        .build())
                .collect(Collectors.toList());

        // 3. 총 결제 금액 계산
        long totalSpent = payments.stream()
                .filter(p -> "PAID".equals(p.getStatus()))
                .mapToLong(PaymentEntity::getAmount)
                .sum();

        return PaymentDTO.HistoryResponse.builder()
                .payments(paymentInfos)
                .totalSpent(totalSpent)
                .build();
    }

    /**
     * [기능: 웹훅 처리 (비동기 결제 반영)]
     * 설명: PG사에서 보내주는 결제 완료 신호를 받아 처리합니다. (가상계좌 입금 확인 등)
     */
    @Transactional
    public void processWebhook(PaymentDTO.WebhookRequest request) {
        String impUid = request.imp_uid();
        String merchantUid = request.merchant_uid();

        log.info("🔔 웹훅 수신: imp_uid={}, merchant_uid={}, status={}", impUid, merchantUid, request.status());

        if (!"paid".equals(request.status())) {
            log.info("결제 완료 상태가 아니므로 무시합니다. status={}", request.status());
            return;
        }

        // 1. 이미 처리된 결제인지 확인 (멱등성 보장)
        PaymentEntity existing = paymentMapper.findByImpUid(impUid);
        if (existing != null && "PAID".equals(existing.getStatus())) {
            log.info("이미 처리된 결제입니다. (Duplicate Webhook)");
            return;
        }

        // 2. 유저 식별 (merchant_uid 포맷: order_{userId}_{timestamp} 가정)
        Long userId = extractUserIdFromMerchantUid(merchantUid);
        if (userId == null) {
            log.error("유저 식별 불가. merchant_uid 형식을 확인하세요: {}", merchantUid);
            return; // 수동 처리 필요
        }

        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            log.error("존재하지 않는 유저 ID입니다: {}", userId);
            return;
        }

        // 3. 검증 및 처리 로직 위임
        // verifyAndUpgrade 내부에서 PortOne API를 다시 호출하여 교차 검증을 수행하므로 안전합니다.
        try {
            PaymentDTO.VerificationRequest verifyRequest = PaymentDTO.VerificationRequest.builder()
                    .impUid(impUid)
                    .merchantUid(merchantUid)
                    // itemName은 Webhook 데이터에 없을 수 있으므로 로직 내 PortOne 조회값 사용 유도
                    .itemName("UNKNOWN")
                    .build();

            // itemName이 "UNKNOWN"이어도 verifyAndUpgrade 내부에서 PortOne API 조회를 통해
            // 실제 결제된 상품명이나 금액을 확인할 수 있어야 함.
            // 하지만 현재 구조상 itemName이 필요하므로, 여기서는 merchant_uid 파싱이나 별도 로직이 필요할 수 있음.
            // *안전한 방식*: verifyAndUpgrade가 PortOne 조회 결과를 우선하도록 설계되었으므로 호출.

            // 주의: verifyAndUpgrade는 itemName을 request에서 가져와 가격을 확인하므로,
            // 웹훅 상황에서는 실제 결제 데이터를 먼저 조회해서 itemName을 채워넣어야 함.

            Map<String, Object> realData = portOneClient.getPayment(impUid);
            String realItemName = (String) realData.get("name"); // PortOne 응답의 상품명 필드

            PaymentDTO.VerificationRequest webhookVerifyRequest = PaymentDTO.VerificationRequest.builder()
                    .impUid(impUid)
                    .merchantUid(merchantUid)
                    .itemName(realItemName) // 실제 상품명 주입
                    .build();

            verifyAndUpgrade(webhookVerifyRequest, user.getUsername());
            log.info("🔔 웹훅을 통한 결제 처리 완료: User={}", user.getUsername());

        } catch (Exception e) {
            log.error("웹훅 처리 중 오류 발생: {}", e.getMessage());
            // 웹훅은 보통 실패 시 재전송되므로 예외를 던지는 것이 맞지만,
            // 무한 루프 방지를 위해 로그만 남기고 종료할 수도 있음 (정책에 따름)
        }
    }

    // --- Private Helper Methods ---

    /**
     * [헬퍼: 상품명에 따른 서버 정가 반환]
     * 이 메서드가 보안의 핵심입니다. 클라이언트가 100원을 보내도 여기서 29900원을 리턴하면 검증에서 걸립니다.
     */
    private int getPriceByItemName(String itemName) {
        if (itemName == null) return 0;
        String normalized = itemName.toUpperCase();

        if (normalized.contains("STANDARD")) {
            return 9900;
        } else if (normalized.contains("PREMIUM")) {
            return 29900;
        }

        return 999999999; // 알 수 없는 상품은 결제되지 않도록 매우 큰 값 반환
    }

    /**
     * [헬퍼: 상품명에 따른 등급 반환]
     */
    private MembershipTier getTierByItemName(String itemName) {
        if (itemName == null) return MembershipTier.BASIC;
        String normalized = itemName.toUpperCase();

        if (normalized.contains("STANDARD")) {
            return MembershipTier.STANDARD;
        } else if (normalized.contains("PREMIUM")) {
            return MembershipTier.PREMIUM;
        }
        return MembershipTier.BASIC;
    }

    /**
     * [헬퍼: 주문번호에서 유저 ID 추출]
     * 포맷: order_{userId}_{timestamp} (예: order_15_1709999999)
     */
    private Long extractUserIdFromMerchantUid(String merchantUid) {
        try {
            if (merchantUid == null) return null;
            String[] parts = merchantUid.split("_");
            if (parts.length >= 2) {
                return Long.parseLong(parts[1]);
            }
        } catch (NumberFormatException e) {
            log.warn("ID 파싱 실패: {}", merchantUid);
        }
        return null;
    }
}