package com.tutoroo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * [기능: 랭킹 데이터 전송 객체]
 * 설명: 랭킹 페이지에서 상위 랭커(Top 3)와 전체 랭킹 리스트를 전달합니다.
 * 참고: Lombok @Builder 패턴을 적용하여 유연한 객체 생성을 지원합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingDTO {

    // 1~3등 (메달 표시용)
    private List<RankEntry> topRankers;

    // 전체 랭킹 리스트 (최대 100명)
    private List<RankEntry> allRankers;

    /**
     * [내부 클래스: 개별 랭킹 정보]
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankEntry {
        private int rank;             // 등수
        private String maskedName;    // 마스킹된 이름 (예: 김*수)
        private int totalPoint;       // 총점
        private String profileImage;  // 프로필 이미지 URL (추가됨)
        private String ageGroup;      // 연령대 (예: 10대, 20대) (추가됨)
    }

    /**
     * [내부 클래스: 랭킹 필터 요청]
     * 설명: 성별, 연령대별 랭킹 조회 시 사용
     */
    @Data
    public static class FilterRequest {
        private String gender;        // MALE, FEMALE
        private Integer ageGroup;     // 10, 20, 30 ...
    }
}