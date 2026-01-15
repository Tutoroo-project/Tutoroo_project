package com.tutoroo.mapper;

import com.tutoroo.entity.PetDiaryEntity;
import com.tutoroo.entity.PetInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PetMapper {

    // 1. 유저 ID로 펫 조회 (개별 조회)
    PetInfoEntity findByUserId(Long userId);

    // 2. [신규] 모든 펫 조회 (스케줄러 가출 체크용)
    List<PetInfoEntity> findAllPets();

    // 3. 펫 생성 (초기 데이터)
    void createPet(PetInfoEntity pet);

    // 4. 펫 상태 업데이트 (모든 상태값 저장)
    void updatePet(PetInfoEntity pet);

    // 5. 다음 진화 단계 조회
    String findNextEvolutionType(@Param("currentStage") int currentStage, @Param("exp") int exp);

    // 6. [AI 감성] 일기 저장
    void saveDiary(PetDiaryEntity diary);

    // 7. [RPG 요소] 스킬 효과 조회
    Double findSkillEffect(@Param("petType") String petType, @Param("skillCode") String skillCode);
}