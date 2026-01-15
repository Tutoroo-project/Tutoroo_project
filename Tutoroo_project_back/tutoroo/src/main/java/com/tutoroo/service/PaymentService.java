package com.tutoroo.service;

import com.tutoroo.dto.PaymentDTO;
import com.tutoroo.entity.MembershipTier;
import com.tutoroo.entity.PaymentEntity;
import com.tutoroo.entity.UserEntity;
import com.tutoroo.mapper.PaymentMapper;
import com.tutoroo.mapper.UserMapper;
import com.tutoroo.util.PortOneClient;
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
    private final PortOneClient portOneClient;

    @Transactional
    public PaymentDTO.VerificationResponse verifyAndUpgrade(PaymentDTO.VerificationRequest request, String username) {
        try {
            UserEntity user = userMapper.findByUsername(username);
            if (user == null) throw new IllegalArgumentException("존재하지 않는 사용자입니다.");

            MembershipTier newTier;
            if (request.amount() == 9900) {
                newTier = MembershipTier.STANDARD;
            } else if (request.amount() == 29900) {
                newTier = MembershipTier.PREMIUM;
            } else {
                throw new IllegalArgumentException("유효하지 않은 결제 금액입니다.");
            }

            user.setMembershipTier(newTier);
            userMapper.update(user);

            PaymentEntity payment = PaymentEntity.builder()
                    .userId(user.getId())
                    .planId(request.planId())
                    .amount(request.amount())
                    .payMethod(request.payMethod())
                    .pgProvider(request.pgProvider())
                    .itemName(newTier.name() + " SUBSCRIPTION")
                    .status("PAID")
                    .paidAt(LocalDateTime.now())
                    .impUid(request.impUid())
                    .merchantUid(request.merchantUid())
                    .build();

            paymentMapper.save(payment);

            return PaymentDTO.VerificationResponse.builder()
                    .success(true)
                    .message(String.format("멤버십이 %s 등급으로 업그레이드 되었습니다.", newTier.name()))
                    .paidAt(LocalDateTime.now().toString())
                    .build();

        } catch (Exception e) {
            log.error("결제 검증 및 등급 반영 실패. 자동 환불 진행: {}", request.impUid(), e);
            try {
                portOneClient.cancelPayment(request.impUid(), "서버 처리 중 오류 자동 취소");
            } catch (Exception cancelEx) {
                log.error("자동 환불 실패. 수동 확인 필요: {}", request.impUid(), cancelEx);
            }
            throw new RuntimeException("결제 처리 중 오류가 발생하여 자동 취소되었습니다.");
        }
    }
}