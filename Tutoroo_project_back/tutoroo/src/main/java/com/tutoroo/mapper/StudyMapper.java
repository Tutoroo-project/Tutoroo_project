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

    // --- [기존 메서드 유지] ---

    /** [기능: 신규 학습 플랜 저장] */
    void savePlan(StudyPlanEntity plan);

    /** [기능: 특정 학습 플랜 조회 (기존)] */
    StudyPlanEntity findPlanById(Long id);

    /** * [기능: 사용자의 활성화된 학습 플랜 리스트 조회]
     * 해결: image_5c66c8.png 오류 해결용
     */
    List<StudyPlanEntity> findActivePlansByUserId(Long userId);

    /** [기능: 학습 플랜 상태 및 로드맵 업데이트 (전체 수정)] */
    void updatePlan(StudyPlanEntity plan);

    /** [기능: 일일 학습 로그(테스트 결과 등) 저장] */
    void saveLog(StudyLogEntity log);

    /** [기능: 특정 플랜의 모든 학습 로그 조회] */
    List<StudyLogEntity> findLogsByPlanId(Long planId);

    // --- [★ 오류 해결을 위해 추가/통합된 메서드] ---

    /** * [기능: ID로 플랜 조회 (서비스 호환용)]
     * 설명: Service에서 findById를 호출하므로 findPlanById와 같은 기능을 하도록 매핑 필요
     */
    StudyPlanEntity findById(Long id);

    /** * [기능: 진도율(Progress)만 부분 업데이트]
     * 사용처: StudyController /progress/{planId} 엔드포인트
     */
    void updateProgress(StudyPlanEntity plan);
}