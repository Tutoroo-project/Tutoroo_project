package com.tutoroo.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptEntity {
    private String promptKey;
    private String content;
    private String description;
}