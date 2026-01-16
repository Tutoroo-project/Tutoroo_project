package com.tutoroo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class NotificationService {

    // 접속한 유저들의 연결 객체 저장소 (Key: userId, Value: Emitter)
    // ConcurrentHashMap을 사용하여 스레드 안전성 보장
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // 기본 타임아웃: 1시간 (3600초 * 1000)
    // Nginx 등 리버스 프록시 사용 시 프록시 설정도 확인 필요
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    /**
     * [기능: SSE 구독 (연결)]
     * 프론트엔드: const eventSource = new EventSource("/api/notifications/subscribe");
     */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(userId, emitter);

        // 콜백 설정: 만료, 타임아웃, 에러 시 맵에서 제거
        emitter.onCompletion(() -> {
            log.debug("SSE 연결 종료 (Completion): {}", userId);
            emitters.remove(userId);
        });
        emitter.onTimeout(() -> {
            log.debug("SSE 연결 타임아웃 (Timeout): {}", userId);
            emitters.remove(userId);
        });
        emitter.onError((e) -> {
            log.debug("SSE 연결 에러 (Error): {}", userId);
            emitters.remove(userId);
        });

        // [중요] 최초 연결 시 더미 데이터 전송
        // 503 Service Unavailable 방지 및 연결 확인용
        sendToClient(userId, "connect", "알림 서버 연결 성공! [UserId: " + userId + "]");

        return emitter;
    }

    /**
     * [기능: 특정 유저에게 알림 발송]
     * 사용처: 스케줄러(가출 알림), 튜터 서비스(피드백 알림) 등
     */
    public void send(Long userId, String message) {
        if (emitters.containsKey(userId)) {
            sendToClient(userId, "notification", message);
        }
    }

    /**
     * [내부 로직: 실제 데이터 전송]
     */
    private void sendToClient(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(userId))
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                // 전송 실패 시 연결이 끊긴 것으로 간주하고 제거
                emitters.remove(userId);
                log.warn("알림 전송 실패 (User: {}): {}", userId, e.getMessage());
            } catch (Exception e) {
                emitters.remove(userId);
                log.error("알림 전송 중 알 수 없는 오류 (User: {}): {}", userId, e.getMessage());
            }
        }
    }
}