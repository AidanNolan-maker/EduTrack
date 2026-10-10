package com.edutrack.backend.dto;

import com.edutrack.backend.entity.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CourseResponse {
    private UUID id;
    private String title;
    private String description;
    private UUID instructorId;
    private String instructorFirstName;
    private String instructorLastName;
    private CourseStatus courseStatus;
}
