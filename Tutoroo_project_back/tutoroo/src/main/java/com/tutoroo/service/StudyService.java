package com.tutoroo.service;

import com.tutoroo.entity.MembershipTier; // ★ 우리가 만든 Enum 임포트 필수
import com.tutoroo.entity.UserEntity;
import com.tutoroo.entity.StudyPlanEntity; // (가정)
import com.tutoroo.exception.TutorooException; // (가정)
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyService {

    private final UserMapper userMapper;
    private final StudyMapper studyMapper;

    // --- [1. 컨트롤러 오류 해결: canCreateNewGoal 추가] ---
    @Transactional(readOnly = true)
    public boolean canCreateNewGoal(Long userId) {
        // validate 메서드를 재사용하거나 로직을 복사
        try {
            validatePlanCreationLimit(userId);
            return true; // 예외가 안 나면 생성 가능
        } catch (Exception e) {
            return false; // 예외(제한 초과)가 나면 생성 불가
        }
    }

    // --- [2. 컨트롤러 오류 해결: updateProgress 추가] ---
    @Transactional
    public void updateProgress(Long planId, Integer rate) {
        // 1. 학습 계획 조회
        StudyPlanEntity plan = studyMapper.findById(planId); // 매퍼 메서드명은 실제 환경에 맞게 수정
        if (plan == null) {
            throw new IllegalArgumentException("존재하지 않는 학습 계획입니다.");
        }

        // 2. 진도율 업데이트
        plan.setProgressRate(rate); // 엔티티에 setProgressRate가 있다고 가정

        // 3. DB 저장 (MyBatis의 경우 update 호출 필요)
        studyMapper.updateProgress(plan);
    }

    // --- [3. 타입 오류 해결: Tier -> MembershipTier로 변경] ---
    @Transactional(readOnly = true)
    public void validatePlanCreationLimit(Long userId) {
        UserEntity user = userMapper.findById(userId);

        // ★ 여기가 문제였음: 반환 타입이 MembershipTier이므로 변수 타입도 맞춰야 함
        MembershipTier tier = user.getEffectiveTier();

        List<StudyPlanEntity> activePlans = studyMapper.findActivePlansByUserId(userId);

        // 멤버십 등급(Enum)에 정의된 maxActiveGoals 활용
        if (activePlans.size() >= tier.getMaxActiveGoals()) {
            // 메시지 포맷팅
            String message = String.format("[%s 등급] 목표는 최대 %d개까지만 설정 가능합니다. (현재: %d개)",
                    tier.name(), tier.getMaxActiveGoals(), activePlans.size());

            // 예외 던지기 (ErrorCode 등은 프로젝트에 맞게 사용)
            throw new RuntimeException(message);
        }
    }
}