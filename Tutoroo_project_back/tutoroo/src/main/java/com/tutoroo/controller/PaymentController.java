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

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/verify")
    public ResponseEntity<PaymentDTO.VerificationResponse> verifyPayment(
            @RequestBody PaymentDTO.VerificationRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String username = (userDetails != null) ? userDetails.getUsername() : "anonymous";
        PaymentDTO.VerificationResponse response = paymentService.verifyAndUpgrade(request, username);

        return ResponseEntity.ok(response);
    }
}