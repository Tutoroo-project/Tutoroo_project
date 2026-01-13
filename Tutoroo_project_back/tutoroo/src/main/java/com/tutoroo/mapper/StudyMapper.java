package com.tutoroo.mapper;

import com.tutoroo.entity.StudyLogEntity;
import com.tutoroo.entity.StudyPlanEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * [기능: 학습 플랜 및 로그 데이터 접근 매퍼]
 * 설명: 플랜 생성, 조회, 업데이트 및 일일 학습 로그 저장을 담당합니다.
 */
@Mapper
public interface StudyMapper {

    // --- [학습 플랜 관련] ---
    void savePlan(StudyPlanEntity plan);
    StudyPlanEntity findPlanById(Long id);
    StudyPlanEntity findById(Long id);
    List<StudyPlanEntity> findActivePlansByUserId(Long userId);
    void updateProgress(StudyPlanEntity plan);
    void updatePlan(StudyPlanEntity plan);

    // --- [학습 로그(Log) 관련] ---
    void saveLog(StudyLogEntity log);
    List<StudyLogEntity> findLogsByPlanId(Long planId);
    StudyLogEntity findLatestLogByPlanId(Long planId);
    void updateStudentFeedback(@Param("planId") Long planId,
                               @Param("dayCount") int dayCount,
                               @Param("feedback") String feedback);

    /** * [신규] 기능: 특정 기간의 학습 로그 조회 (Step 19 시험 출제용)
     * 설명: startDay ~ endDay 사이의 'dailySummary'를 모아서 시험 문제를 출제합니다.
     */
    List<StudyLogEntity> findLogsBetweenDays(@Param("planId") Long planId,
                                             @Param("startDay") int startDay,
                                             @Param("endDay") int endDay);
}