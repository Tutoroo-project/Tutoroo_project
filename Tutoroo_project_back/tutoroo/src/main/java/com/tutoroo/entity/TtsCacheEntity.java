package com.tutoroo.entity;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TtsCacheEntity {
    private Long id;
    private String textHash;
    private String audioBase64;
    private LocalDateTime createdAt;
}