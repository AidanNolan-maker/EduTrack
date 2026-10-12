package com.edutrack.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CourseProgressResponse {
    private UUID courseId;
    private String courseTitle;
    private int totalLessons;
    private int completedLessons;
    private double progressPercentage;
}
