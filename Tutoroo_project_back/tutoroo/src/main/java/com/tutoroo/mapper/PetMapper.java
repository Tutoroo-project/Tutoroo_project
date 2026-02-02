package com.tutoroo.mapper;

import com.tutoroo.entity.PetDiaryEntity;
import com.tutoroo.entity.PetInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PetMapper {
    PetInfoEntity findByUserId(Long userId);
    List<PetInfoEntity> findAllByUserId(Long userId);
    PetInfoEntity findById(Long petId);
    List<PetInfoEntity> findAllActivePets();
    void createPet(PetInfoEntity pet);
    void updatePet(PetInfoEntity pet);
    Integer findRequiredExpForNextStage(int stage);
    void saveDiary(PetDiaryEntity diary);
    Double findSkillEffect(@Param("petType") String petType, @Param("skillCode") String skillCode);
    void deleteByUserId(Long userId);
    List<PetDiaryEntity> findAllDiariesByUserId(Long userId);
}