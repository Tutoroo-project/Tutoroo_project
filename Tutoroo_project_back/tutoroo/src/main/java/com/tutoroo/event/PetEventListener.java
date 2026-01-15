package com.tutoroo.event;

import com.tutoroo.service.PetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PetEventListener {

    private final PetService petService;

    @EventListener
    public void handleStudyCompleted(StudyCompletedEvent event) {
        // 시험 점수가 60점 이상이면 펫 경험치 지급
        if (event.getScore() >= 60) {
            int expGain = event.getScore() / 2; // 예: 100점 -> 50 경험치
            log.info("학습 보상: 펫 경험치 +{}", expGain);
            petService.gainExp(event.getUserId(), expGain);
        }
    }
}