package com.tutoroo.controller;

import com.tutoroo.dto.PaymentDTO;
import com.tutoroo.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [기능: 결제 API 컨트롤러]
 * 설명: 프론트엔드로부터 결제 완료 신호를 받아 서버 검증을 수행합니다.
 */
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * [기능: 멤버십 결제 검증 및 등급 반영 API]
     * URL: POST /api/payment/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentDTO.VerificationResponse> verifyPayment(
            @RequestBody PaymentDTO.VerificationRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // 인증 정보가 없을 경우 처리 (익명 등)
        String username = (userDetails != null) ? userDetails.getUsername() : "anonymous";

        // 서비스의 verifyAndUpgrade 호출
        PaymentDTO.VerificationResponse response = paymentService.verifyAndUpgrade(request, username);

        return ResponseEntity.ok(response);
    }
}