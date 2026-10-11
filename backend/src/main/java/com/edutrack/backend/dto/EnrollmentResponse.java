package com.edutrack.backend.dto;

import com.edutrack.backend.entity.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class EnrollmentResponse {
    private UUID id;
    private UUID courseId;
    private String courseTitle;
    private UUID studentId;
    private String studentFirstName;
    private String studentLastName;
    private EnrollmentStatus status;
    private LocalDateTime enrolledAt;
}
