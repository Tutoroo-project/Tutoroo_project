package com.tutoroo.service;

import com.tutoroo.dto.PetDTO;
import com.tutoroo.entity.*;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.PetMapper;
import com.tutoroo.mapper.UserMapper;
import com.tutoroo.util.FileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetService {

    private final PetMapper petMapper;
    private final UserMapper userMapper;
    private final ChatClient.Builder chatClientBuilder;
    private final ImageModel imageModel;
    private final FileStore fileStore;
    private final RedisTemplate<String, String> redisTemplate;

    private static final int FULLNESS_DECAY_PER_HOUR = 5;
    private static final int INTIMACY_DECAY_PER_HOUR = 3;
    private static final int RUNAWAY_THRESHOLD = 20;

    // 상호작용 비용 및 효과 상수
    private static final int COST_FEED = 20;
    private static final int EXP_FEED = 5;
    private static final int EXP_PLAY = 10;
    private static final int EXP_CLEAN = 5;

    // --- [1] 펫 상태 조회 ---
    @Transactional(readOnly = true)
    public PetDTO.PetStatusResponse getPetStatus(Long userId) {
        PetInfoEntity pet = petMapper.findByUserId(userId);
        if (pet == null) return null;

        updatePetStats(pet);
        petMapper.updatePet(pet);

        int maxExp = petMapper.findRequiredExpForNextStage(pet.getStage());
        return mapToDTO(pet, maxExp);
    }

    // --- [2] 초기 입양 ---
    @Transactional(readOnly = true)
    public PetDTO.AdoptableListResponse getAdoptablePets(Long userId) {
        UserEntity user = userMapper.findById(userId);
        Set<PetType> allowedPets = user.getEffectiveTier().getAllowedPets();

        List<PetDTO.PetSummary> summaries = allowedPets.stream()
                .map(type -> new PetDTO.PetSummary(type.name(), type.getName(), type.getDescription()))
                .toList();

        return PetDTO.AdoptableListResponse.builder()
                .availablePets(summaries)
                .message(String.format("회원님의 %s 등급에서는 %d마리의 펫을 선택할 수 있습니다.",
                        user.getEffectiveTier().name(), summaries.size()))
                .build();
    }

    @Transactional
    public void adoptInitialPet(Long userId, String petTypeStr) {
        if (petMapper.findByUserId(userId) != null) {
            throw new TutorooException(ErrorCode.ALREADY_HAS_PET);
        }

        PetType type;
        try {
            type = PetType.valueOf(petTypeStr);
        } catch (IllegalArgumentException e) {
            throw new TutorooException(ErrorCode.INVALID_PET_TYPE);
        }

        PetInfoEntity newPet = PetInfoEntity.builder()
                .userId(userId)
                .petName(type.getName())
                .petType(type.name())
                .stage(1)
                .status("ACTIVE")
                .fullness(80)
                .intimacy(80)
                .exp(0)
                .cleanliness(100)
                .stress(0)
                .energy(100)
                .isSleeping(false)
                .createdAt(LocalDateTime.now())
                .lastFedAt(LocalDateTime.now())
                .lastPlayedAt(LocalDateTime.now())
                .lastCleanedAt(LocalDateTime.now())
                .lastSleptAt(LocalDateTime.now())
                .build();

        petMapper.createPet(newPet);
    }

    // --- [3] 상호작용 (밥주기, 놀기 등) ---
    @Transactional
    public PetDTO.PetStatusResponse interact(Long userId, String actionType) {
        PetInfoEntity pet = petMapper.findByUserId(userId);
        if (pet == null) throw new TutorooException(ErrorCode.PET_NOT_FOUND);

        // 시간 경과에 따른 스탯 감소 적용
        updatePetStats(pet);

        // 자고 있는 경우 깨우기 외에는 불가능
        if (pet.isSleeping() && !"WAKE_UP".equals(actionType)) {
            throw new TutorooException("펫이 자고 있습니다. 먼저 깨워주세요.", ErrorCode.PET_IS_SLEEPING);
        }

        UserEntity user = userMapper.findById(userId);

        switch (actionType) {
            case "FEED" -> {
                if (user.getPointBalance() < COST_FEED) {
                    throw new TutorooException(ErrorCode.INSUFFICIENT_POINTS);
                }
                userMapper.spendPoints(userId, COST_FEED);

                pet.setFullness(Math.min(100, pet.getFullness() + 30));
                pet.setExp(pet.getExp() + EXP_FEED);
                pet.setLastFedAt(LocalDateTime.now());
            }
            case "PLAY" -> {
                if (pet.getEnergy() < 10) {
                    throw new TutorooException("펫이 너무 피곤해합니다. 잠을 재워주세요.", ErrorCode.PET_TOO_TIRED);
                }
                pet.setIntimacy(Math.min(100, pet.getIntimacy() + 15));
                pet.setStress(Math.max(0, pet.getStress() - 10));
                pet.setEnergy(Math.max(0, pet.getEnergy() - 10));
                pet.setExp(pet.getExp() + EXP_PLAY);
                pet.setLastPlayedAt(LocalDateTime.now());
            }
            case "CLEAN" -> {
                pet.setCleanliness(100);
                pet.setStress(Math.max(0, pet.getStress() - 20));
                pet.setExp(pet.getExp() + EXP_CLEAN);
                pet.setLastCleanedAt(LocalDateTime.now());
            }
            case "SLEEP" -> {
                pet.setSleeping(true); // [수정됨] setIsSleeping -> setSleeping
                pet.setLastSleptAt(LocalDateTime.now());
            }
            case "WAKE_UP" -> {
                pet.setSleeping(false); // [수정됨] setIsSleeping -> setSleeping

                long sleptHours = Duration.between(pet.getLastSleptAt(), LocalDateTime.now()).toHours();
                int recovery = (int) (sleptHours * 10) + 20;
                pet.setEnergy(Math.min(100, pet.getEnergy() + recovery));
            }
            default -> throw new TutorooException(ErrorCode.INVALID_INPUT_VALUE);
        }

        checkLevelUp(pet);
        petMapper.updatePet(pet);

        int maxExp = petMapper.findRequiredExpForNextStage(pet.getStage());
        return mapToDTO(pet, maxExp);
    }

    // --- [4] 경험치 획득 (이벤트 등 외부 호출용) ---
    @Transactional
    public void gainExp(Long userId, int amount) {
        PetInfoEntity pet = petMapper.findByUserId(userId);
        if (pet == null || !"ACTIVE".equals(pet.getStatus())) return;

        pet.setExp(pet.getExp() + amount);
        checkLevelUp(pet);
        petMapper.updatePet(pet);
    }

    // --- [5] 가출 체크 ---
    @Transactional
    public List<Long> processBatchRunaways() {
        List<PetInfoEntity> activePets = petMapper.findAllActivePets();
        List<Long> runawayUserIds = new ArrayList<>();

        for (PetInfoEntity pet : activePets) {
            updatePetStats(pet);
            if (pet.getIntimacy() <= RUNAWAY_THRESHOLD) {
                pet.setStatus("RUNAWAY");
                runawayUserIds.add(pet.getUserId());
                log.info("🚨 펫 가출 발생! PetId: {}", pet.getPetId());
            }
            petMapper.updatePet(pet);
        }
        return runawayUserIds;
    }

    // --- [6] 미드나잇 다이어리 (AI 그림 일기) ---
    @Transactional
    public void writeMidnightDiary(Long userId) {
        PetInfoEntity pet = petMapper.findByUserId(userId);
        if (pet == null || !"ACTIVE".equals(pet.getStatus())) return;

        try {
            // 1. 일기 내용 생성
            String prompt = String.format("""
                    너는 %s야. 오늘 주인님과 함께한 하루를 짧은 일기(3문장)로 써줘.
                    현재 기분: %s (배고픔: %d, 친밀도: %d).
                    말투는 귀엽게 해.
                    """, pet.getPetName(), (pet.getIntimacy() > 80 ? "행복함" : "심심함"), pet.getFullness(), pet.getIntimacy());

            String content = chatClientBuilder.build().prompt().user(prompt).call().content();

            // 2. 일기 그림 생성 (DALL-E)
            String imagePrompt = String.format(
                    "A cute %s character looking %s, simple vector art style, pastel colors, white background",
                    pet.getPetType().toLowerCase(),
                    (pet.getIntimacy() > 80 ? "happy" : "sad")
            );

            ImageResponse imageResponse = imageModel.call(new ImagePrompt(imagePrompt,
                    OpenAiImageOptions.builder()
                            .withN(1)
                            .withHeight(1024)
                            .withWidth(1024)
                            .build()));

            String imageUrl = imageResponse.getResult().getOutput().getUrl();

            String diaryContent = content + "\n\n![그림일기](" + imageUrl + ")";

            // 3. DB 저장
            PetDiaryEntity diary = PetDiaryEntity.builder()
                    .petId(pet.getPetId())
                    .date(LocalDate.now())
                    .content(diaryContent)
                    .mood(pet.getIntimacy() > 80 ? "HAPPY" : "SAD")
                    .createdAt(LocalDateTime.now())
                    .build();

            petMapper.saveDiary(diary);
            log.info("📖 펫 일기 작성 완료: {}", pet.getPetId());

        } catch (Exception e) {
            log.error("일기 생성 실패: {}", e.getMessage());
        }
    }

    // --- [7] 졸업 후 알 관련 ---
    public PetDTO.RandomEggResponse getGraduationEggs(Long userId) {
        return null;
    }

    public void hatchEgg(Long userId, String selectedPetType) {
        // 부화 로직
    }

    // --- 내부 메서드 ---

    private void checkLevelUp(PetInfoEntity pet) {
        int required = petMapper.findRequiredExpForNextStage(pet.getStage());
        if (pet.getExp() >= required && pet.getStage() < 5) {
            pet.setStage(pet.getStage() + 1);
            pet.setExp(pet.getExp() - required);
            log.info("🎉 펫 진화! UserId: {}, NewStage: {}", pet.getUserId(), pet.getStage());
        } else if (pet.getStage() == 5 && pet.getExp() >= required) {
            pet.setStatus("GRADUATED");
        }
    }

    private void updatePetStats(PetInfoEntity pet) {
        LocalDateTime now = LocalDateTime.now();

        long hFed = Duration.between(pet.getLastFedAt(), now).toHours();
        if(hFed > 0) pet.setFullness(Math.max(0, pet.getFullness() - (int)hFed * FULLNESS_DECAY_PER_HOUR));

        long hPlay = Duration.between(pet.getLastPlayedAt(), now).toHours();
        if(hPlay > 0) pet.setIntimacy(Math.max(0, pet.getIntimacy() - (int)hPlay * INTIMACY_DECAY_PER_HOUR));
    }

    private PetDTO.PetStatusResponse mapToDTO(PetInfoEntity pet, int maxExp) {
        return PetDTO.PetStatusResponse.builder()
                .petId(pet.getPetId())
                .petName(pet.getPetName())
                .petType(pet.getPetType())
                .stage(pet.getStage())
                .fullness(pet.getFullness())
                .intimacy(pet.getIntimacy())
                .exp(pet.getExp())
                .maxExp(maxExp)
                .cleanliness(pet.getCleanliness())
                .stress(pet.getStress())
                .energy(pet.getEnergy())
                .isSleeping(pet.isSleeping())
                .status(pet.getStatus())
                .statusMessage(generateRandomMessage(pet))
                .build();
    }

    private String generateRandomMessage(PetInfoEntity pet) {
        if (pet.isSleeping()) return "Zzz... (세상 모르게 자고 있다)";
        if ("GRADUATED".equals(pet.getStatus())) return "당신 덕분에 훌륭하게 자랐어요!";
        if (pet.getFullness() < 30) return "배가 고파요... 밥 주세요.";
        if (pet.getIntimacy() < 30) return "심심해요. 놀아주세요.";
        return "오늘도 기분 좋은 하루예요!";
    }
}