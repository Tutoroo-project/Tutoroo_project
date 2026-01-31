package com.tutoroo.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * [기능: 문제 유형 정의 Enum (확장판)]
 * 설명: 코딩, 객관식뿐만 아니라 예체능(그림, 노래)까지 포함하는 범용 타입 정의
 */
@Getter
@RequiredArgsConstructor
public enum QuestionType {

    // 1. 기본형
    MULTIPLE_CHOICE("객관식", "주어진 보기 중 정답을 하나 선택"),
    SHORT_ANSWER("단답형", "핵심 키워드나 짧은 문장으로 답하기"),
    LONG_ANSWER("서술형", "논리적인 글쓰기나 에세이 작성"),

    // 2. 코딩형
    CODE_FILL_IN("코드 빈칸 채우기", "주어진 코드의 빈칸 채우기"),
    CODE_IMPLEMENTATION("코드 구현", "요구사항에 맞는 전체 코드 작성"),

    // 3. 예체능 & 실습형 (입출력이 파일인 경우)
    DRAWING_SUBMISSION("그림 제출", "주제에 맞는 그림을 그려서 이미지로 제출 (미술, 도형)"),
    AUDIO_RECORDING("음성 녹음", "발음, 노래, 연주 등을 녹음하여 제출 (언어, 음악)"),
    VIDEO_SUBMISSION("영상 제출", "운동 자세나 실습 과정을 촬영하여 제출 (체육)"),

    // 4. 시각자료 분석형 (이미지/도표/그래프 보고 해석)
    VISUAL_ANALYSIS("시각 자료 분석", "이미지/도표/그래프를 보고 해석/추론");

    private final String description;
    private final String detail;
}