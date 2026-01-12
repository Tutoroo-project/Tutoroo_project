package com.tutoroo.controller;

import com.tutoroo.dto.RankingDTO;
import com.tutoroo.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [기능: 랭킹 API 컨트롤러]
 * 설명: 실시간 랭킹(Redis)과 조건별 랭킹(DB)을 분리하여 제공합니다.
 */
@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    /**
     * 1. 실시간 전체 랭킹 조회 (Redis 사용)
     * URL: GET /api/ranking/realtime
     * 특징: 속도가 매우 빠름 (메인 화면 추천)
     */
    @GetMapping("/realtime")
    public ResponseEntity<RankingDTO> getRealtimeRanking() {
        return ResponseEntity.ok(rankingService.getRealtimeRankings());
    }

    /**
     * 2. 조건별 필터링 랭킹 조회 (DB 사용)
     * URL: GET /api/ranking/list?gender=MALE&ageGroup=20
     * 특징: 성별, 연령대별 검색 가능
     */
    @GetMapping("/list")
    public ResponseEntity<RankingDTO> getFilteredRankings(@ModelAttribute RankingDTO.FilterRequest filter) {
        return ResponseEntity.ok(rankingService.getFilteredRankings(filter));
    }
}