package com.tutoroo.entity;

import lombok.*;

/**
 * [기능: 랭킹 정보 조회용 객체]
 * 설명: 실시간 랭킹 순위와 사용자 요약 정보를 포함합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RankingEntity {
    private Integer rank;           // 순위
    private String name;            // 마스킹된 이름
    private Integer totalPoint;     // 누적 포인트
    private String profileImage;    // 프로필 사진
    private String ageGroup;        // 연령대 필터링용
    private String gender;          // 성별 필터링용
}