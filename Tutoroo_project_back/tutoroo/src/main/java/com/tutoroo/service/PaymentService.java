package com.tutoroo.service;

import com.tutoroo.dto.PaymentDTO;
import com.tutoroo.entity.MembershipTier;
import com.tutoroo.entity.PaymentEntity;
import com.tutoroo.entity.UserEntity;
import com.tutoroo.mapper.PaymentMapper;
import com.tutoroo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserMapper userMapper;
    private final PaymentMapper paymentMapper;

    /**
     * [기능: 멤버십 구독 결제 검증 및 등급 업그레이드]
     */
    @Transactional
    public PaymentDTO.VerificationResponse verifyAndUpgrade(PaymentDTO.VerificationRequest request, String username) {
        // 1. 유저 조회
        UserEntity user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        // 2. 금액에 따른 등급 결정 (정책 하드코딩)
        MembershipTier newTier;
        if (request.getAmount() == 9900) {
            newTier = MembershipTier.STANDARD;
        } else if (request.getAmount() == 29900) {
            newTier = MembershipTier.PREMIUM;
        } else {
            // 예외 처리 또는 기본 로직
            throw new IllegalArgumentException("유효하지 않은 결제 금액입니다: " + request.getAmount());
        }

        // 3. 등급 적용
        user.setMembershipTier(newTier);

        // (필요 시 SubscriptionEndDate 연장 로직 추가)
        // user.setSubscriptionEndDate(LocalDateTime.now().plusMonths(1));

        // 유저 정보 업데이트
        userMapper.update(user);

        // 4. 결제 내역 저장
        PaymentEntity payment = PaymentEntity.builder()
                .userId(user.getId())
                .planId(request.getPlanId()) // Plan ID가 있다면 저장
                .amount(request.getAmount())
                .payMethod(request.getPayMethod())
                .pgProvider(request.getPgProvider())
                .itemName(newTier.name() + " SUBSCRIPTION")
                .status("PAID")
                .paidAt(LocalDateTime.now())
                .impUid(request.getImpUid())
                .merchantUid(request.getMerchantUid())
                .build();

        paymentMapper.save(payment);

        log.info("멤버십 결제 성공: user={}, tier={}, amount={}", username, newTier, request.getAmount());

        // 5. 응답 생성
        return PaymentDTO.VerificationResponse.builder()
                .success(true)
                .message(String.format("멤버십이 %s 등급으로 업그레이드 되었습니다.", newTier.name()))
                .paidAt(LocalDateTime.now().toString())
                // .nextPaymentDate(...) // 필요 시 계산하여 주입
                .build();
    }
}