package com.tutoroo.service;

import com.tutoroo.dto.PetDTO;
import com.tutoroo.entity.*;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.PetMapper;
import com.tutoroo.mapper.StudyMapper;
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

import java.io.InputStream;
import java.net.URL;
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
    private final StudyMapper studyMapper;
    private final ChatClient.Builder chatClientBuilder;
    private final ImageModel imageModel;
    private final FileStore fileStore;

    // 상수 설정
    private static final int FULLNESS_DECAY_PER_HOUR = 5;
    private static final int INTIMACY_DECAY_PER_HOUR = 3;
    private static final int RUNAWAY_THRESHOLD = 20;

    private static final int COST_FEED = 20;
    private static final int EXP_FEED = 5;
    private static final int EXP_PLAY = 10;
    private static final int EXP_CLEAN = 5;

    // --- [1] 펫 상태 조회 ---
    public PetDTO.PetStatusResponse getPetStatus(Long userId) {
        PetInfoEntity pet = petMapper.findByUserId(userId);
        if (pet == null || "RUNAWAY".equals(pet.getStatus())) {
            return null;
        }

        updatePetStats(pet);
        petMapper.updatePet(pet);

        int maxExp = petMapper.findRequiredExpForNextStage(pet.getStage());
        if (pet.getStage() >= 5) maxExp = 999999;

        return mapToDTO(pet, maxExp);
    }

    // --- [2] 입양 가능한 펫 목록 조회 ---
    @Transactional(readOnly = true)
    public PetDTO.AdoptableListResponse getAdoptablePets(Long userId) {
        UserEntity user = userMapper.findById(userId);
        Set<PetType> allowedPets = user.getEffectiveTier().getAllowedPets();

        List<PetDTO.PetSummary> summaries = allowedPets.stream()
                .map(type -> new PetDTO.PetSummary(type.name(), type.getName(), type.getDescription()))
                .toList();

        return PetDTO.AdoptableListResponse.builder()
                .availablePets(summaries)
                .message("현재 등급에서 입양 가능한 펫 목록입니다.")
                .build();
    }

    // --- [3] 초기 펫 입양 ---
    @Transactional
    public void adoptInitialPet(Long userId, String petTypeStr, String inputName) {
        PetInfoEntity existingPet = petMapper.findByUserId(userId);

        if (existingPet != null) {
            //상태가 가출(RUNAWAY)이면 -> 기존 펫 삭제 후 진행
            if ("RUNAWAY".equals(existingPet.getStatus())) {
                petMapper.deleteByUserId(userId);
            } else {
                // 가출도 아닌데 또 만들려고 하면 에러 발생
                throw new TutorooException(ErrorCode.ALREADY_HAS_PET);
            }
        }

        // 2. 펫 타입 확인 및 생성 로직 (기존 코드 유지)
        PetType type;
        try {
            type = PetType.valueOf(petTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new TutorooException(ErrorCode.INVALID_PET_TYPE);
        }

        UserEntity user = userMapper.findById(userId);
        if (!user.getEffectiveTier().getAllowedPets().contains(type)) {
            throw new TutorooException(ErrorCode.MEMBERSHIP_PET_RESTRICTION);
        }

        String finalName = (inputName == null || inputName.isBlank()) ? type.getName() : inputName;
        createPetEntity(userId, type, finalName, null, null);
    }

    // --- [4] 상호작용 ---
    @Transactional
    public PetDTO.PetStatusResponse interact(Long userId, String actionType) {
        PetInfoEntity pet = petMapper.findByUserId(userId);
        if (pet == null) throw new TutorooException(ErrorCode.PET_NOT_FOUND);

        updatePetStats(pet);

        if (pet.isSleeping() && !"WAKE_UP".equals(actionType)) {
            throw new TutorooException(ErrorCode.PET_IS_SLEEPING);
        }

        UserEntity user = userMapper.findById(userId);

        switch (actionType) {
            case "FEED" -> {
                if (user.getPointBalance() < COST_FEED) throw new TutorooException(ErrorCode.INSUFFICIENT_POINTS);
                userMapper.spendPoints(userId, COST_FEED);
                pet.setFullness(Math.min(100, pet.getFullness() + 30));
                pet.setExp(pet.getExp() + EXP_FEED);
                pet.setLastFedAt(LocalDateTime.now());
            }
            case "PLAY" -> {
                if (pet.getEnergy() < 10) throw new TutorooException(ErrorCode.PET_TOO_TIRED);
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
                pet.setSleeping(true);
                pet.setLastSleptAt(LocalDateTime.now());
            }
            case "WAKE_UP" -> {
                pet.setSleeping(false);
                long sleptHours = Duration.between(pet.getLastSleptAt(), LocalDateTime.now()).toHours();
                int recovery = (int) (sleptHours * 10) + 20;
                pet.setEnergy(Math.min(100, pet.getEnergy() + recovery));
            }
            default -> throw new TutorooException(ErrorCode.INVALID_INPUT_VALUE);
        }

        checkLevelUp(pet);
        petMapper.updatePet(pet);

        int maxExp = petMapper.findRequiredExpForNextStage(pet.getStage());
        if (pet.getStage() >= 5) maxExp = 999999;
        return mapToDTO(pet, maxExp);
    }

    // --- [New] 외부(이벤트) 경험치 지급 메서드 추가 ---
    @Transactional
    public void gainExp(Long userId, int amount) {
        PetInfoEntity pet = petMapper.findByUserId(userId);
        if (pet == null) return; // 펫이 없으면 패스

        // 경험치 증가
        pet.setExp(pet.getExp() + amount);

        // 레벨업 체크
        checkLevelUp(pet);

        // DB 저장
        petMapper.updatePet(pet);
    }

    // --- [5] 졸업 후 알 관련 ---
    @Transactional(readOnly = true)
    public PetDTO.RandomEggResponse getGraduationEggs(Long userId) {
        PetInfoEntity currentPet = petMapper.findByUserId(userId);
        if (currentPet != null) {
            throw new TutorooException(ErrorCode.ALREADY_HAS_PET);
        }

        UserEntity user = userMapper.findById(userId);
        MembershipTier tier = user.getEffectiveTier();

        List<PetDTO.PetSummary> candidates = new ArrayList<>();
        List<PetType> allowed = new ArrayList<>(tier.getAllowedPets());
        Collections.shuffle(allowed);
        allowed.stream().limit(2).forEach(type ->
                candidates.add(new PetDTO.PetSummary(type.name(), type.getName(), "새로운 운명의 만남"))
        );
        candidates.add(new PetDTO.PetSummary("CUSTOM_EGG", "신비로운 무지개 알", "당신의 상상력으로 태어나는 펫"));

        return PetDTO.RandomEggResponse.builder()
                .candidates(candidates)
                .choiceCount(1)
                .build();
    }

    @Transactional
    public void hatchEgg(Long userId, String selectedPetType, String inputName) {
        if (petMapper.findByUserId(userId) != null) throw new TutorooException(ErrorCode.ALREADY_HAS_PET);
        if ("CUSTOM_EGG".equals(selectedPetType)) throw new TutorooException("커스텀 펫 생성 API를 이용해주세요.", ErrorCode.INVALID_INPUT_VALUE);

        PetType type;
        try {
            type = PetType.valueOf(selectedPetType);
        } catch (IllegalArgumentException e) {
            throw new TutorooException(ErrorCode.INVALID_PET_TYPE);
        }

        String finalName = (inputName == null || inputName.isBlank()) ? type.getName() : inputName;

        createPetEntity(userId, type, finalName, null, null);
    }

    // --- [6] 커스텀 펫 생성 (Step 20) ---
    @Transactional
    public void createCustomPet(Long userId, PetDTO.CustomPetCreateRequest request) {
        if (petMapper.findByUserId(userId) != null) throw new TutorooException(ErrorCode.ALREADY_HAS_PET);

        // DTO 필드명 수정 반영 (name -> petName, description -> customDescription)
        String imagePrompt = String.format(
                "A cute, simple, vector-style flat illustration of a pet. Concept: %s. Base animal: %s. White background.",
                request.customDescription(), request.baseType()
        );

        String finalImageUrl = "/images/pets/default_custom.png";
        try {
            ImageResponse response = imageModel.call(new ImagePrompt(imagePrompt,
                    OpenAiImageOptions.builder().withModel("dall-e-3").withHeight(1024).withWidth(1024).build()));

            String originalUrl = response.getResult().getOutput().getUrl();
            try (InputStream in = new URL(originalUrl).openStream()) {
                finalImageUrl = fileStore.storeFile(in.readAllBytes(), ".png");
            }
        } catch (Exception e) {
            log.error("이미지 생성 실패", e);
        }

        // Entity 생성 호출
        createPetEntity(userId, PetType.CUSTOM, request.petName(), request.customDescription(), finalImageUrl);
    }

    // --- [7] 미드나잇 다이어리 ---
    @Transactional
    public void writeMidnightDiary(Long userId) {
        PetInfoEntity pet = petMapper.findByUserId(userId);
        if (pet == null) return;

        // 1. 오늘 공부 기록 가져오기 (StudyMapper 사용)
        List<StudyLogEntity> todayLogs = studyMapper.findLogsByUserIdAndDate(userId, LocalDate.now());

        // 2. 공부 내용 요약하기
        String dailyActivity;
        if (todayLogs.isEmpty()) {
            dailyActivity = "오늘은 공부 기록이 없어. 주인님이 바빴나봐.";
        } else {
            // 공부 내용과 AI 피드백 등을 콤마로 연결해서 문자열로 만듦
            StringBuilder sb = new StringBuilder();
            for (StudyLogEntity log : todayLogs) {
                sb.append("[공부내용: ").append(log.getContentSummary()).append("] ");
            }
            dailyActivity = "오늘 공부 기록이야: " + sb.toString();
        }

        try {
            // 3. AI에게 상황극 시키기 (프롬프트 수정)
            String prompt = String.format(
                    "너는 지금부터 '%s'(이)라는 이름의 펫이야.\n" +
                            "오늘 주인님의 하루 정보: [%s].\n\n" +
                            "이 정보를 보고 너의 시점에서 '비밀 관찰 일기'를 써줘.\n" +
                            "다음 규칙을 꼭 지켜:\n" +
                            "1. 말투: 어린 아이처럼 아주 귀엽게, 반말로, 이모지(😊, 🔥)를 많이 섞어서.\n" +
                            "2. 시점: 주인님한테 말을 거는 게 아니라, '오늘 주인님이 ~를 했다' 식의 혼잣말.\n" +
                            "3. 형식:\n" +
                            "   제목: [오늘 내용에 어울리는 엉뚱하고 귀여운 제목]\n" +
                            "   날씨: [오늘 기분으로 날씨 표현]\n" +
                            "   내용: [3~4줄 정도의 일기 본문]",
                    pet.getPetName(),
                    dailyActivity // <--- 여기에 공부 기록이 들어감!
            );

            // 4. AI 호출 및 저장 (기존 코드와 동일)
            String content = chatClientBuilder.build().prompt().user(prompt).call().content();

            PetDiaryEntity diary = PetDiaryEntity.builder()
                    .petId(pet.getPetId())
                    .date(LocalDate.now())
                    .content(content)
                    .mood("HAPPY")
                    .createdAt(LocalDateTime.now())
                    .build();
            petMapper.saveDiary(diary);

        } catch (Exception e) {
            log.error("일기 작성 실패", e);
        }
    }

    // --- [8] 가출 체크 ---
    @Transactional
    public List<Long> processBatchRunaways() {
        List<PetInfoEntity> activePets = petMapper.findAllActivePets();
        List<Long> runawayUserIds = new ArrayList<>();
        for (PetInfoEntity pet : activePets) {
            updatePetStats(pet);
            if (pet.getIntimacy() <= RUNAWAY_THRESHOLD) {
                pet.setStatus("RUNAWAY");
                runawayUserIds.add(pet.getUserId());
            }
            petMapper.updatePet(pet);
        }
        return runawayUserIds;
    }

    // --- Helper Methods ---
    private void createPetEntity(Long userId, PetType type, String name, String customDesc, String customImg) {
        PetInfoEntity newPet = PetInfoEntity.builder()
                .userId(userId)
                .petName(name)
                .petType(type.name()) // Enum -> String 변환 저장
                .customDescription(customDesc)
                .customImageUrl(customImg)
                .stage(1)
                .status("ACTIVE")
                .fullness(80).intimacy(80).exp(0).cleanliness(100).stress(0).energy(100)
                .isSleeping(false)
                .createdAt(LocalDateTime.now())
                .lastFedAt(LocalDateTime.now()).lastPlayedAt(LocalDateTime.now())
                .lastCleanedAt(LocalDateTime.now()).lastSleptAt(LocalDateTime.now())
                .build();
        petMapper.createPet(newPet);
    }

    private void checkLevelUp(PetInfoEntity pet) {
        if (pet.getStage() >= 5) return;
        Integer required = petMapper.findRequiredExpForNextStage(pet.getStage());
        if (required != null && pet.getExp() >= required) {
            pet.setStage(pet.getStage() + 1);
            pet.setExp(pet.getExp() - required);
            if (pet.getStage() == 5) pet.setStatus("GRADUATED");
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
                .customImageUrl(pet.getCustomImageUrl()) // Entity 필드명과 일치
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
                .statusMessage(pet.getStatus().equals("GRADUATED") ? "졸업을 축하합니다!" : "오늘도 행복해요!")
                .build();
    }

    // --- [9] 내 일기장 목록 조회 ---
    @Transactional(readOnly = true)
    public List<PetDTO.PetDiaryResponse> getMyDiaries(Long userId) {
        // 1. DB에서 내 펫의 일기 다 가져오기
        List<PetDiaryEntity> diaries = petMapper.findAllDiariesByUserId(userId);

        // 2. DTO로 변환해서 반환
        return diaries.stream()
                .map(diary -> PetDTO.PetDiaryResponse.builder()
                        .diaryId(diary.getDiaryid())
                        .date(diary.getDate().toString())
                        .content(diary.getContent())
                        .mood(diary.getMood())
                        .build())
                .toList();
    }
}