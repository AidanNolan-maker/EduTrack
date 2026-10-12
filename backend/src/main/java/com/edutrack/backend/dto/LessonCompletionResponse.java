package com.edutrack.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class LessonCompletionResponse {
    private UUID id;
    private UUID lessonId;
    private UUID studentId;
    private LocalDateTime completedAt;
}
