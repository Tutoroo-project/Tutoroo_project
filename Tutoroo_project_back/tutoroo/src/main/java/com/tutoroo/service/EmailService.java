package com.tutoroo.service;

import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final RedisTemplate<String, String> redisTemplate;

    // 인증번호 유효시간 (5분)
    private static final long CODE_EXPIRATION_SECONDS = 300;

    /**
     * [기능 1] 인증번호 생성 및 이메일 발송
     */
    public void sendVerificationCode(String email) {
        String code = createCode();
        String subject = "[Tutoroo] 본인 확인 인증번호 안내";
        String content = String.format("""
                <div style="font-family: 'Apple SD Gothic Neo', sans-serif; padding: 20px;">
                    <h2 style="color: #4A90E2;">Tutoroo 인증번호</h2>
                    <p>안녕하세요, Tutoroo입니다.</p>
                    <p>아래 인증번호 6자리를 입력하여 본인 인증을 완료해주세요.</p>
                    <div style="background: #f0f0f0; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px; margin: 20px 0;">
                        %s
                    </div>
                    <p style="color: #888;">이 인증번호는 5분간 유효합니다.</p>
                </div>
                """, code);

        sendMail(email, subject, content);

        // Redis에 저장
        redisTemplate.opsForValue().set(
                "AUTH:" + email,
                code,
                CODE_EXPIRATION_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /**
     * [기능 2] 임시 비밀번호 이메일 발송 (신규 추가)
     */
    public void sendTemporaryPassword(String email, String tempPassword) {
        String subject = "[Tutoroo] 임시 비밀번호 발급 안내";
        String content = String.format("""
                <div style="font-family: 'Apple SD Gothic Neo', sans-serif; padding: 20px;">
                    <h2 style="color: #E24A4A;">Tutoroo 임시 비밀번호</h2>
                    <p>안녕하세요, Tutoroo입니다.</p>
                    <p>요청하신 임시 비밀번호가 발급되었습니다.</p>
                    <p>로그인 후 반드시 비밀번호를 변경해주세요.</p>
                    <div style="background: #fff0f0; padding: 15px; text-align: center; font-size: 20px; font-weight: bold; margin: 20px 0; border: 1px solid #ffcccc;">
                        %s
                    </div>
                </div>
                """, tempPassword);

        sendMail(email, subject, content);
    }

    /**
     * [기능 3] 인증번호 검증
     */
    public boolean verifyCode(String email, String inputCode) {
        String cachedCode = redisTemplate.opsForValue().get("AUTH:" + email);
        if (cachedCode != null && cachedCode.equals(inputCode)) {
            redisTemplate.delete("AUTH:" + email);
            return true;
        }
        return false;
    }

    // --- 내부 헬퍼 메서드 ---

    private void sendMail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // HTML 여부 true

            javaMailSender.send(message);
            log.info("메일 발송 성공: {}", to);

        } catch (MessagingException e) {
            log.error("메일 발송 실패: {}", e.getMessage());
            throw new TutorooException("메일 발송 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String createCode() {
        return String.valueOf(new Random().nextInt(900000) + 100000);
    }
}