package com.tutoroo.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudyCompletedEvent {
    private Long userId;
    private int score;
}