package com.edutrack.backend.controller;

import com.edutrack.backend.dto.CourseResponse;
import com.edutrack.backend.dto.CreateCourseRequest;
import com.edutrack.backend.entity.Course;
import com.edutrack.backend.mapper.CourseMapper;
import com.edutrack.backend.security.CustomUserDetails;
import com.edutrack.backend.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;
    private final CourseMapper courseMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<CourseResponse> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID instructorId = userDetails.getUser().getId();

        Course course = courseService.createCourse(
                request,
                instructorId
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(courseMapper.toResponse(course));
    }

    @GetMapping("/published")
    public ResponseEntity<List<CourseResponse>> getPublishedCourses() {
        List<CourseResponse> courses = courseService
                .getPublishedCourses()
                .stream()
                .map(courseMapper::toResponse)
                .toList();

        return ResponseEntity.ok(courses);
    }

    @GetMapping("/my-courses")
    public ResponseEntity<List<CourseResponse>> getMyCourses(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID instructorId = userDetails.getUser().getId();

        List<CourseResponse> courses = courseService
                .getCoursesByInstructor(instructorId)
                .stream()
                .map(courseMapper::toResponse)
                .toList();

        return ResponseEntity.ok(courses);
    }

    @PatchMapping("/{courseId}/publish")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<CourseResponse> publishCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUser().getId();

        Course course = courseService.publishCourse(
                courseId,
                userId
        );

        return ResponseEntity.ok(courseMapper.toResponse(course));
    }
}
