package com.tutoroo.service;

import com.tutoroo.dto.PetDTO;
import com.tutoroo.entity.PetDiaryEntity;
import com.tutoroo.entity.PetInfoEntity;
import com.tutoroo.entity.StudyLogEntity;
import com.tutoroo.entity.UserEntity;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.mapper.PetMapper;
import com.tutoroo.mapper.StudyMapper;
import com.tutoroo.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class PetService {

    private final PetMapper petMapper;
    private final UserMapper userMapper;
    private final StudyMapper studyMapper;
    private final ChatClient chatClient; // AI 일기 생성용

    // 밸런스 상수 설정
    private static final int FULLNESS_DECAY_PER_HOUR = 5;
    private static final int INTIMACY_DECAY_PER_HOUR = 3;
    private static final int COST_FEED = 10;

    // [핵심] 생성자 주입 방식으로 ChatClient.Builder 사용 (오류 해결됨)
    public PetService(PetMapper petMapper,
                      UserMapper userMapper,
                      StudyMapper studyMapper,
                      ChatClient.Builder chatClientBuilder) {
        this.petMapper = petMapper;
        this.userMapper = userMapper;
        this.studyMapper = studyMapper;
        this.chatClient = chatClientBuilder
                .defaultSystem("너는 사용자의 귀여운 펫이야. 친구처럼 반말을 사용해.")
                .build();
    }

    @Transactional
    public PetDTO.PetStatusResponse getPetStatus(Long userId) {
        PetInfoEntity pet = petMapper.findByUserId(userId);
        if (pet == null) pet = createInitialPet(userId);

        // [개별 가출 체크] 3일(72시간) 이상 방치 시 가출 -> 초기화
        if (checkRunaway(pet)) {
            return PetDTO.PetStatusResponse.builder()
                    .petName("가출함")
                    .statusMessage("펫이 외로움을 견디지 못하고 떠났습니다... (초기화됨)")
                    .stage(0).petType("NONE").build();
        }

        applyPassiveDecay(pet); // 시간 경과에 따른 상태 변화 적용
        petMapper.updatePet(pet);
        return mapToDTO(pet);
    }

    @Transactional
    public PetDTO.PetStatusResponse interact(Long userId, String actionType) {
        PetInfoEntity pet = petMapper.findByUserId(userId);
        UserEntity user = userMapper.findById(userId);

        if (pet == null) throw new TutorooException("펫이 없습니다.", ErrorCode.INVALID_INPUT_VALUE);

        applyPassiveDecay(pet); // 최신 상태 반영

        // [수면 방해 금지] 자는데 깨우는 거(WAKE_UP) 아니면 접근 금지
        if (pet.isSleeping() && !"WAKE_UP".equals(actionType)) {
            pet.setStress(Math.min(100, pet.getStress() + 20));
            pet.setIntimacy(Math.max(0, pet.getIntimacy() - 10));
            petMapper.updatePet(pet);
            throw new TutorooException("Zzz... 펫이 자고 있어요! 깨우면 화냅니다.", ErrorCode.INVALID_INPUT_VALUE);
        }

        switch (actionType) {
            case "FEED":
                if (user.getTotalPoint() < COST_FEED) throw new TutorooException("포인트 부족", ErrorCode.INVALID_INPUT_VALUE);
                userMapper.updateUserPoint(userId, user.getTotalPoint() - COST_FEED);

                pet.setFullness(Math.min(100, pet.getFullness() + 20));
                pet.setLastFedAt(LocalDateTime.now());

                // [디테일] 밥 먹으면 30% 확률로 똥 쌈
                if (new Random().nextInt(100) < 30) {
                    pet.setCleanliness(Math.max(0, pet.getCleanliness() - 40));
                }
                break;

            case "PLAY":
                if (pet.getEnergy() < 10) throw new TutorooException("너무 피곤해해요.", ErrorCode.INVALID_INPUT_VALUE);
                pet.setIntimacy(Math.min(100, pet.getIntimacy() + 15));
                pet.setStress(Math.max(0, pet.getStress() - 10));
                pet.setEnergy(Math.max(0, pet.getEnergy() - 10));
                pet.setLastPlayedAt(LocalDateTime.now());
                break;

            case "CLEAN":
                pet.setCleanliness(100);
                pet.setStress(Math.max(0, pet.getStress() - 5));
                pet.setLastCleanedAt(LocalDateTime.now());
                break;

            case "SLEEP":
                pet.setSleeping(true);
                pet.setLastSleptAt(LocalDateTime.now());
                break;

            case "WAKE_UP":
                pet.setSleeping(false);
                break;
        }

        checkEvolution(pet);
        petMapper.updatePet(pet);
        return mapToDTO(pet);
    }

    // [학습 보상 연동] 경험치 획득
    @Transactional
    public void gainExp(Long userId, int amount) {
        PetInfoEntity pet = petMapper.findByUserId(userId);
        if (pet != null) {
            applyPassiveDecay(pet);
            // 위생 상태가 나쁘면 경험치 획득량 50% 감소
            if (pet.getCleanliness() < 30) amount /= 2;

            pet.setExp(pet.getExp() + amount);
            checkEvolution(pet);
            petMapper.updatePet(pet);
        }
    }

    // [RPG 요소] 포인트 버프 배율 계산 (TutorService에서 호출)
    public double getPointMultiplier(Long userId) {
        PetInfoEntity pet = petMapper.findByUserId(userId);
        if (pet == null) return 1.0;

        // 친밀도 70 이상 & 깨어있음 & 아프지 않음(위생 50이상) 일 때만 버프 발동
        if (pet.getIntimacy() >= 70 && !pet.isSleeping() && pet.getCleanliness() >= 50) {
            Double skillEffect = petMapper.findSkillEffect(pet.getPetType(), "POINT_BOOST");
            return skillEffect != null ? skillEffect : 1.0;
        }
        return 1.0;
    }

    // [AI 감성] 밤 12시에 실행될 일기 쓰기 (스케줄러 호출용)
    @Transactional
    public void writeMidnightDiary(Long userId) {
        PetInfoEntity pet = petMapper.findByUserId(userId);
        if (pet == null) return;

        List<StudyLogEntity> logs = studyMapper.findLogsByUserIdAndDate(userId, LocalDate.now());

        String mood = pet.getIntimacy() > 60 ? "행복함" : "슬픔";
        String prompt = String.format("""
                너는 5살짜리 '%s' 캐릭터야. 이름은 '%s'.
                오늘 주인님이 공부를 %d번 했어.
                네 현재 기분은 '%s', 배부름은 %d%%야.
                오늘 하루를 관찰한 3줄짜리 귀여운 그림일기를 써줘. 반말을 써.
                """, pet.getPetType(), pet.getPetName(), logs != null ? logs.size() : 0, mood, pet.getFullness());

        String content = chatClient.prompt().user(prompt).call().content();

        petMapper.saveDiary(PetDiaryEntity.builder()
                .petId(pet.getPetId())
                .date(LocalDate.now())
                .content(content)
                .mood(mood.equals("행복함") ? "HAPPY" : "SAD")
                .build());
    }

    // [스케줄러용] 전체 펫을 일괄 검사하고 가출한 유저 ID 목록 반환 (실시간 알림용)
    @Transactional
    public List<Long> processBatchRunaways() {
        List<PetInfoEntity> allPets = petMapper.findAllPets();
        List<Long> runawayUserIds = new ArrayList<>();

        for (PetInfoEntity pet : allPets) {
            // 이미 가출 상태(EGG이고 초기상태)면 패스
            if ("EGG".equals(pet.getPetType()) && pet.getStage() == 1 && pet.getIntimacy() == 50) {
                continue;
            }

            // 가출 조건 체크 (3일 방치 + 친밀도 0)
            long hours = Duration.between(pet.getLastPlayedAt(), LocalDateTime.now()).toHours();
            if (hours > 72 && pet.getIntimacy() == 0) {
                // 가출 확정 -> 초기화
                pet.setStage(1);
                pet.setPetType("EGG");
                pet.setPetName("새로운 알");
                pet.setFullness(80);
                pet.setIntimacy(50);
                pet.setExp(0);

                petMapper.updatePet(pet); // DB 업데이트
                runawayUserIds.add(pet.getUserId()); // 알림 보낼 대상에 추가
                log.info("🚨 펫 가출 발생! User: {}", pet.getUserId());
            }
        }
        return runawayUserIds;
    }

    // --- 내부 로직 (Private) ---

    private void applyPassiveDecay(PetInfoEntity pet) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 수면 중: 에너지 회복, 8시간 지나면 자동 기상
        if (pet.isSleeping()) {
            long hours = Duration.between(pet.getLastSleptAt(), now).toHours();
            if (hours > 0) pet.setEnergy(Math.min(100, pet.getEnergy() + (int)hours * 10));
            if (hours >= 8) pet.setSleeping(false);
            pet.setLastSleptAt(now);
            return;
        }

        // 2. 활동 중: 배고픔, 친밀도 감소
        long hFed = Duration.between(pet.getLastFedAt(), now).toHours();
        if(hFed > 0) pet.setFullness(Math.max(0, pet.getFullness() - (int)hFed * FULLNESS_DECAY_PER_HOUR));

        long hPlay = Duration.between(pet.getLastPlayedAt(), now).toHours();
        if(hPlay > 0) pet.setIntimacy(Math.max(0, pet.getIntimacy() - (int)hPlay * INTIMACY_DECAY_PER_HOUR));
    }

    // 개별 펫 가출 체크 (getPetStatus용)
    private boolean checkRunaway(PetInfoEntity pet) {
        long hours = Duration.between(pet.getLastPlayedAt(), LocalDateTime.now()).toHours();
        if (hours > 72 && pet.getIntimacy() == 0) {
            pet.setStage(1);
            pet.setPetType("EGG");
            pet.setPetName("새로운 알");
            pet.setFullness(80);
            pet.setIntimacy(50);
            pet.setExp(0);
            petMapper.updatePet(pet);
            return true;
        }
        return false;
    }

    private void checkEvolution(PetInfoEntity pet) {
        String nextType = petMapper.findNextEvolutionType(pet.getStage(), pet.getExp());
        if (nextType != null) {
            pet.setStage(pet.getStage() + 1);
            pet.setPetType(nextType);
            pet.setExp(0);
            log.info("펫 진화! {} -> {}", pet.getUserId(), nextType);
        }
    }

    private PetInfoEntity createInitialPet(Long userId) {
        PetInfoEntity p = PetInfoEntity.builder()
                .userId(userId).petName("알")
                .fullness(80).intimacy(50).cleanliness(100).energy(100)
                .stage(1).petType("EGG")
                .lastFedAt(LocalDateTime.now()).lastPlayedAt(LocalDateTime.now()).lastCleanedAt(LocalDateTime.now())
                .build();
        petMapper.createPet(p);
        return p;
    }

    private PetDTO.PetStatusResponse mapToDTO(PetInfoEntity pet) {
        return PetDTO.PetStatusResponse.builder()
                .petName(pet.getPetName())
                .fullness(pet.getFullness())
                .intimacy(pet.getIntimacy())
                .cleanliness(pet.getCleanliness())
                .stress(pet.getStress())
                .energy(pet.getEnergy())
                .isSleeping(pet.isSleeping())
                .stage(pet.getStage())
                .petType(pet.getPetType())
                .statusMessage(pet.isSleeping() ? "Zzz..." : "주인님 놀아줘요!")
                .build();
    }
}