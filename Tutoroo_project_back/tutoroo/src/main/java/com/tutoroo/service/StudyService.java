package com.tutoroo.service;

import com.tutoroo.entity.MembershipTier;
import com.tutoroo.entity.StudyPlanEntity;
import com.tutoroo.entity.UserEntity;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class StudyService {

    private final UserMapper userMapper;
    private final StudyMapper studyMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional(readOnly = true)
    public boolean canCreateNewGoal(Long userId) {
        try {
            validatePlanCreationLimit(userId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public void updateProgress(Long planId, Integer rate) {
        StudyPlanEntity plan = studyMapper.findById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("존재하지 않는 학습 계획입니다.");
        }
        plan.setProgressRate(rate);
        studyMapper.updateProgress(plan);
    }

    // [요구사항 4] 멤버십 등급에 따른 플랜 생성 제한
    @Transactional(readOnly = true)
    public void validatePlanCreationLimit(Long userId) {
        UserEntity user = userMapper.findById(userId);
        MembershipTier tier = user.getEffectiveTier();
        List<StudyPlanEntity> activePlans = studyMapper.findActivePlansByUserId(userId);

        if (activePlans.size() >= tier.getMaxActiveGoals()) {
            // 프론트에서 이 에러코드를 받으면 결제 모달을 띄우도록 약속됨
            throw new TutorooException(ErrorCode.MULTIPLE_PLANS_REQUIRED_PAYMENT);
        }
    }

    // [요구사항 5] 수업 진행 상태 실시간 저장 (Redis 활용)
    // 키: session:{planId}, 값: JSON (step, chatHistory 등)
    public void saveSessionState(Long planId, String stateJson) {
        String key = "session:" + planId;
        // 24시간 동안 유지
        redisTemplate.opsForValue().set(key, stateJson, 24, TimeUnit.HOURS);
    }

    // 중단된 수업 상태 불러오기
    public String getSessionState(Long planId) {
        return redisTemplate.opsForValue().get("session:" + planId);
    }

    // 수업 완료 시 상태 삭제
    public void clearSessionState(Long planId) {
        redisTemplate.delete("session:" + planId);
    }
}