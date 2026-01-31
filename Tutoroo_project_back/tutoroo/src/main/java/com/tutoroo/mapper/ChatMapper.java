package com.tutoroo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ChatMapper {
    // 대화 저장
    void saveMessage(@Param("planId") Long planId, @Param("sender") String sender, @Param("message") String message);

    // 최근 대화 불러오기 (limit 개수만큼)
    List<ChatMessage> findRecentMessages(@Param("planId") Long planId, @Param("limit") int limit);

    // 내부 DTO
    record ChatMessage(String sender, String message) {}
}