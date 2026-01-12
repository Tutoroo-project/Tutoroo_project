package com.tutoroo.mapper;

import com.tutoroo.entity.PromptEntity;
import com.tutoroo.entity.TtsCacheEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommonMapper {
    // 1. 프롬프트 조회
    String findPromptContentByKey(String key);

    // 2. TTS 캐시 조회
    TtsCacheEntity findTtsCacheByHash(String textHash);

    // 3. TTS 캐시 저장
    void saveTtsCache(TtsCacheEntity ttsCache);
}