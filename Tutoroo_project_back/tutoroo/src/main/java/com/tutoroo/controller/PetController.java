package com.tutoroo.controller;

import com.tutoroo.dto.PetDTO;
import com.tutoroo.security.CustomUserDetails;
import com.tutoroo.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pet")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    // 1. 현재 펫 상태 조회 (메인 화면, 대시보드)
    @GetMapping("/status")
    public ResponseEntity<PetDTO.PetStatusResponse> getStatus(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(petService.getPetStatus(user.getId()));
    }

    // 2. [초기 입양] 입양 가능한 펫 목록 조회
    @GetMapping("/adoptable")
    public ResponseEntity<PetDTO.AdoptableListResponse> getAdoptableList(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(petService.getAdoptablePets(user.getId()));
    }

    // 3. [초기 입양] 펫 선택 및 생성 요청
    @PostMapping("/adopt")
    public ResponseEntity<String> adoptPet(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody PetDTO.InitialAdoptRequest request) {
        petService.adoptInitialPet(user.getId(), request.petType(), request.petName());
        return ResponseEntity.ok("새로운 펫을 입양했습니다! 사랑으로 키워주세요.");
    }

    // 4. [졸업 후] 랜덤 알 후보 조회
    @GetMapping("/eggs")
    public ResponseEntity<PetDTO.RandomEggResponse> getEggs(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(petService.getGraduationEggs(user.getId()));
    }

    // 5. [졸업 후] 알 선택 및 부화 요청
    @PostMapping("/hatch")
    public ResponseEntity<String> hatchEgg(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody PetDTO.EggSelectRequest request) {
        petService.hatchEgg(user.getId(), request.selectedPetType(), request.petName());
        return ResponseEntity.ok("알이 부화했습니다! 새로운 친구와 여정을 시작하세요.");
    }

    // 6. 상호작용 (밥주기: 포인트 차감)
    @PostMapping("/interact")
    public ResponseEntity<PetDTO.PetStatusResponse> interact(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody PetDTO.InteractionRequest request) {
        return ResponseEntity.ok(petService.interact(user.getId(), request.actionType()));
    }

    // 7. [New] 커스텀 펫 생성 (Step 20)
    // 졸업 조건을 만족해야 호출 가능합니다.
    @PostMapping("/create-custom")
    public ResponseEntity<String> createCustomPet(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody PetDTO.CustomPetCreateRequest request) {
        petService.createCustomPet(user.getId(), request);
        return ResponseEntity.ok("나만의 커스텀 펫이 탄생했습니다!");
    }

    @GetMapping("/diaries")
    public ResponseEntity<List<PetDTO.PetDiaryResponse>> getDiaries(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(petService.getMyDiaries(user.getId()));
    }

//-------------------------------------------------------------------
//    @GetMapping("/test/diary")
//    public ResponseEntity<String> testDiary(@AuthenticationPrincipal CustomUserDetails user) {
//        try {
//            petService.writeMidnightDiary(user.getId());
//            return ResponseEntity.ok("✅ 테스트 성공! DB의 pet_diary 테이블을 확인해보세요.");
//        } catch (Exception e) {
//            return ResponseEntity.ok("❌ 실패: " + e.getMessage());
//        }
//    }
}
