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
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // 1. 프론트엔드가 구독(연결) 요청 시 호출
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 연결 시간 무제한 설정
        emitters.put(userId, emitter);

        // 연결 종료/타임아웃 시 목록에서 제거
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((e) -> emitters.remove(userId));

        // 최초 연결 시 더미 데이터 전송 (연결 확인용)
        sendToClient(userId, "connect", "알림 서버 연결 성공!");

        return emitter;
    }

    // 2. 특정 유저에게 알림 발송 (스케줄러에서 사용)
    public void send(Long userId, String message) {
        if (emitters.containsKey(userId)) {
            sendToClient(userId, "notification", message);
        }
    }

    // 내부 전송 로직
    private void sendToClient(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                emitters.remove(userId); // 전송 실패 시 연결 끊긴 것으로 간주
                log.error("알림 전송 실패 user={}: {}", userId, e.getMessage());
            }
        }
    }
}