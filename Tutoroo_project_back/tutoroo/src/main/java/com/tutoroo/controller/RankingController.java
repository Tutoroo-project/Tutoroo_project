package com.tutoroo.controller;

import com.tutoroo.dto.RankingDTO;
import com.tutoroo.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/realtime")
    public ResponseEntity<RankingDTO> getRealtimeRanking() {
        return ResponseEntity.ok(rankingService.getRealtimeRankings());
    }

    @GetMapping("/list")
    public ResponseEntity<RankingDTO> getFilteredRankings(@ModelAttribute RankingDTO.FilterRequest filter) {
        return ResponseEntity.ok(rankingService.getFilteredRankings(filter));
    }
}