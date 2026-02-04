package com.tutoroo.controller;

import com.tutoroo.security.CustomUserDetails;
import com.tutoroo.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/study/review") // [수정] 프론트엔드 경로 (/api/study/review)와 일치시킴
@RequiredArgsConstructor
@Tag(name = "Review PDF", description = "복습 자료 PDF 생성 및 다운로드")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/download") // [수정] 최종 URL: /api/study/review/download
    @Operation(summary = "일일 복습 리포트 PDF 다운로드", description = "특정 Day의 학습 요약과 피드백을 PDF로 다운로드합니다.")
    public ResponseEntity<byte[]> downloadReviewPdf(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam Long planId,      // [수정] Query Param으로 변경
            @RequestParam Integer dayCount  // [수정] Query Param으로 변경
    ) {
        // 1. PDF 생성 (Day 정보 전달)
        byte[] pdfBytes = reviewService.generateDailyReviewPdf(user.getId(), planId, dayCount);

        // 2. 파일명 생성 (예: Tutoroo_Review_Day5.pdf)
        String fileName = "Tutoroo_Review_Day" + dayCount + "_" + LocalDate.now() + ".pdf";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        // 3. 응답 반환
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                .body(pdfBytes);
    }
}