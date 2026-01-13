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

        // 2. 금액에 따른 등급 결정 (하드코딩된 금액 정책)
        MembershipTier newTier;
        if (request.getAmount() == 9900) {
            newTier = MembershipTier.STANDARD;
        } else if (request.getAmount() == 29900) {
            newTier = MembershipTier.PREMIUM;
        } else {
            throw new IllegalArgumentException("유효하지 않은 결제 금액입니다: " + request.getAmount());
        }

        // 3. 등급 적용 및 기간 연장 (1개월)
        user.setMembershipTier(newTier);

        // [주의] UserEntity에 subscriptionEndDate 필드가 추가되어야 합니다.
        // 만약 필드가 없다면 이 부분 로직은 UserEntity 수정 후 주석 해제하세요.
        /*
        LocalDateTime now = LocalDateTime.now();
        if (user.getSubscriptionEndDate() != null && user.getSubscriptionEndDate().isAfter(now)) {
            user.setSubscriptionEndDate(user.getSubscriptionEndDate().plusMonths(1));
        } else {
            user.setSubscriptionEndDate(now.plusMonths(1));
        }
        */

        // 유저 정보 업데이트 (등급 변경 반영)
        // 이전 단계의 UserMapper.xml에 있는 update 쿼리 사용
        userMapper.update(user);

        // 4. 결제 내역 저장 (PaymentEntity 빌더 패턴 사용)
        PaymentEntity payment = PaymentEntity.builder()
                .userId(user.getId())
                .amount(request.getAmount())
                .payMethod(request.getPayMethod())
                .itemName(newTier.name() + " SUBSCRIPTION") // 상품명 설정
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
                // .nextPaymentDate(user.getSubscriptionEndDate().toString()) // 필드 있으면 추가
                .build();
    }
}