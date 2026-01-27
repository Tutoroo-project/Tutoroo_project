package com.tutoroo.mapper;

import com.tutoroo.entity.StudyLogEntity;
import com.tutoroo.entity.StudyPlanEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface StudyMapper {

    // --- [1. 학습 플랜] ---
    void savePlan(StudyPlanEntity plan);
    StudyPlanEntity findById(Long id);
    StudyPlanEntity findPlanById(Long id);
    List<StudyPlanEntity> findActivePlansByUserId(Long userId);
    int countActivePlansByUserId(Long userId);
    void updateProgress(StudyPlanEntity plan);
    void updatePlan(StudyPlanEntity plan);
    void deletePlan(Long id); // [New] 삭제 메서드 추가

    // --- [2. 학습 로그] ---
    void saveLog(StudyLogEntity log);
    List<StudyLogEntity> findLogsByPlanId(Long planId);
    void updateStudentFeedback(@Param("planId") Long planId,
                               @Param("dayCount") int dayCount,
                               @Param("feedback") String feedback);
    List<StudyLogEntity> findLogsBetweenDays(@Param("planId") Long planId,
                                             @Param("startDay") int startDay,
                                             @Param("endDay") int endDay);

    void updateAiFeedbackPending(@Param("logId") Long logId);

    void updateAiFeedbackSuccess(@Param("logId") Long logId,
                                 @Param("aiFeedback") String aiFeedback);

    void updateAiFeedbackFailed(@Param("logId") Long logId);
    StudyLogEntity findLatestLogByPlanId(@Param("planId") Long planId);
    // --- [3. 펫 다마고치 연동] ---
    List<StudyLogEntity> findLogsByUserIdAndDate(@Param("userId") Long userId,
                                                 @Param("date") LocalDate date);
    // --- [4. (New) 캘린더 조회용] ---
    // 유저의 모든 플랜에 대한 특정 월의 로그 조회
    List<StudyLogEntity> findLogsByUserIdAndMonth(@Param("userId") Long userId,
                                                  @Param("year") int year,
                                                  @Param("month") int month);
}