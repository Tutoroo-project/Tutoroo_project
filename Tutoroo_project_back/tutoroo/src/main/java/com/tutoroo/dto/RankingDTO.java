package com.tutoroo.dto;

import lombok.Builder;
import java.util.List;

/**
 * [기능: 랭킹 데이터 전송 객체 (Record 변환 완료)]
 * 설명: 랭킹 페이지에서 상위 랭커(Top 3)와 전체 랭킹 리스트를 전달합니다.
 */
@Builder
public record RankingDTO(
        // 1~3등 (메달 표시용)
        List<RankEntry> topRankers,

        // 전체 랭킹 리스트 (최대 100명)
        List<RankEntry> allRankers
) {

    /**
     * [내부 레코드: 개별 랭킹 정보]
     */
    @Builder
    public record RankEntry(
            int rank,             // 등수
            String maskedName,    // 마스킹된 이름 (예: 김*수)
            int totalPoint,       // 총점
            String profileImage,  // 프로필 이미지 URL
            String ageGroup       // 연령대 (예: 10대, 20대)
    ) {}

    /**
     * [내부 레코드: 랭킹 필터 요청]
     * 설명: 성별, 연령대별 랭킹 조회 시 사용 (@ModelAttribute 바인딩 호환)
     */
    public record FilterRequest(
            String gender,    // MALE, FEMALE
            Integer ageGroup  // 10, 20, 30...
    ) {}
}