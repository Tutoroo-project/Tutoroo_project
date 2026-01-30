package com.tutoroo.mapper;

import com.tutoroo.entity.PracticeLogEntity;
import com.tutoroo.entity.PracticeQuestionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PracticeMapper {

    // 1. 문제 중복 확인
    int countByContentHash(String contentHash);

    // 2. 문제 저장
    void saveQuestion(PracticeQuestionEntity question);

    // 3. 문제 조회
    PracticeQuestionEntity findQuestionById(Long id);

    // 4. 로그 저장
    void saveLog(PracticeLogEntity log);

    // 5. 약점 분석 (가장 많이 틀린 토픽 TOP 5)
    List<String> findTopWeakTopics(@Param("userId") Long userId, @Param("planId") Long planId);

    // 6. 특정 토픽의 과거 문제 조회 (복습용)
    List<PracticeQuestionEntity> findWrongQuestionsByTopic(@Param("userId") Long userId, @Param("topic") String topic);
}