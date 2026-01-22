package com.tutoroo.controller;

import com.tutoroo.dto.RivalDTO;
import com.tutoroo.dto.UserDTO;
import com.tutoroo.exception.ErrorCode;
import com.tutoroo.exception.TutorooException;
import com.tutoroo.security.CustomUserDetails;
import com.tutoroo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "회원 정보 및 라이벌 관리")
public class UserController {

    private final UserService userService;

    // 1. 회원 상세 정보 조회
    @GetMapping("/profile")
    @Operation(summary = "프로필 정보 조회", description = "마이페이지 수정 화면 진입 시 사용합니다.")
    public ResponseEntity<UserDTO.ProfileInfo> getProfileInfo(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(userService.getProfileInfo(user.getUsername()));
    }

    // 2. 대시보드 조회
    @GetMapping("/dashboard")
    @Operation(summary = "대시보드 조회", description = "메인 화면의 학습 현황 및 요약 정보를 반환합니다.")
    public ResponseEntity<UserDTO.DashboardDTO> getDashboard(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(userService.getAdvancedDashboard(user.getUsername()));
    }

    // 3. 회원 정보 수정
    @PatchMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "회원 정보 수정", description = "프로필 이미지 및 개인정보를 수정합니다.")
    public ResponseEntity<UserDTO.ProfileInfo> updateUserInfo(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart(value = "data") UserDTO.UpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);

        UserDTO.UpdateResponse response = userService.updateUserInfo(user.getUsername(), request, image);
        return ResponseEntity.ok(response.after());
    }

    // 4. 라이벌 매칭 요청
    @PostMapping("/match-rival")
    @Operation(summary = "라이벌 매칭 요청", description = "비슷한 점수대의 유저를 찾아 라이벌로 등록합니다.")
    public ResponseEntity<String> matchRival(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        String result = userService.matchRival(user.getId());
        return ResponseEntity.ok(result);
    }

    // 5. [New] 라이벌 상세 조회 (비교)
    @GetMapping("/rival")
    @Operation(summary = "라이벌 현황 조회", description = "나와 라이벌의 점수 및 랭킹을 비교합니다.")
    public ResponseEntity<RivalDTO.RivalComparisonResponse> getRivalComparison(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(userService.getRivalComparison(user.getId()));
    }

    // 6. 회원 탈퇴 요청
    @PostMapping("/withdraw")
    @Operation(summary = "회원 탈퇴", description = "계정을 비활성화하고 90일 후 영구 삭제합니다.")
    public ResponseEntity<String> withdraw(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody UserDTO.WithdrawRequest request) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);
        userService.withdrawUser(user.getId(), request);
        return ResponseEntity.ok("회원 탈퇴 처리가 완료되었습니다. 90일 후 데이터가 영구 삭제됩니다.");
    }

    // 7. 비밀번호 검증 API
    @PostMapping("/verify-password")
    @Operation(summary = "비밀번호 검증", description = "중요 정보 수정 전 비밀번호를 재확인합니다. 소셜 유저는 자동 통과됩니다.")
    public ResponseEntity<String> verifyPassword(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody UserDTO.PasswordVerifyRequest request) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);

        // NPE 방지를 위해 비밀번호가 null이면 빈 문자열로 처리
        String passwordToCheck = request.password() != null ? request.password() : "";
        userService.verifyPassword(user.getId(), passwordToCheck);

        return ResponseEntity.ok("비밀번호 인증 성공");
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 간편 조회", description = "결제창 호출 시 필요한 유저 ID(PK)와 연락처 정보를 반환합니다.")
    public ResponseEntity<UserDTO.PaymentUserInfo> getMySimpleInfo(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);

        // CustomUserDetails에 ID가 있으므로 바로 Service 호출
        return ResponseEntity.ok(userService.getPaymentUserInfo(user.getId()));
    }

    @PatchMapping("/change-password")
    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 확인 후 새 비밀번호로 변경합니다.")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody UserDTO.PasswordChangeRequest request
    ) {
        if (user == null) throw new TutorooException(ErrorCode.UNAUTHORIZED_ACCESS);

        userService.changePassword(user.getId(), request);
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }
}