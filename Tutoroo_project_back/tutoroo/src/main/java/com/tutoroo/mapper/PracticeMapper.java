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

    // [New] 7. 특정 날짜의 오답 상세 조회 (PDF용)
    // 문제 내용(JSON), 본인 답안, AI 해설을 한 번에 가져옵니다.
    List<WrongPracticeDetail> findWrongLogDetailsByDate(@Param("userId") Long userId, @Param("date") String date);

    // PDF 생성을 위한 내부 DTO (Record)
    record WrongPracticeDetail(
            String questionJson, // 문제 내용
            String userAnswer,   // 내가 쓴 답
            String aiFeedback    // AI 해설
    ) {}
}