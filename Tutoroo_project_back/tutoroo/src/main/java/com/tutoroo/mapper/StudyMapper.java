package com.tutoroo.mapper;

import com.tutoroo.entity.StudyLogEntity;
import com.tutoroo.entity.StudyPlanEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface StudyMapper {

    // --- [1. 학습 플랜 (StudyService 필수)] ---

    // 플랜 생성
    void savePlan(StudyPlanEntity plan);

    // ID로 플랜 조회 (Service에서 findById 호출함)
    StudyPlanEntity findById(Long id);

    // (보조) 명시적 이름의 조회 메서드
    StudyPlanEntity findPlanById(Long id);

    // 활성화된 플랜 목록 조회 (생성 제한 확인용)
    List<StudyPlanEntity> findActivePlansByUserId(Long userId);

    // 활성화된 플랜 개수 카운트 (최적화용)
    int countActivePlansByUserId(Long userId);

    // 진도율 업데이트 (Service에서 updateProgress 호출함)
    void updateProgress(StudyPlanEntity plan);

    // 플랜 정보 전체 업데이트 (튜터 이름 변경 등)
    void updatePlan(StudyPlanEntity plan);


    // --- [2. 학습 로그 (TutorService 필수)] ---

    // 로그 저장
    void saveLog(StudyLogEntity log);

    // 플랜별 로그 목록 조회
    List<StudyLogEntity> findLogsByPlanId(Long planId);

    // 학생 피드백 저장
    void updateStudentFeedback(@Param("planId") Long planId,
                               @Param("dayCount") int dayCount,
                               @Param("feedback") String feedback);

    // 시험 출제용 (특정 구간 로그 조회)
    List<StudyLogEntity> findLogsBetweenDays(@Param("planId") Long planId,
                                             @Param("startDay") int startDay,
                                             @Param("endDay") int endDay);

    // --- [3. 펫 다마고치 연동 (PetService 필수)] ---

    // 펫 일기 쓰기용: 유저의 특정 날짜 학습 기록 조회 (JOIN)
    List<StudyLogEntity> findLogsByUserIdAndDate(@Param("userId") Long userId,
                                                 @Param("date") LocalDate date);
}