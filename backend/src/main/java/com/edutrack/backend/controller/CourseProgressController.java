package com.edutrack.backend.controller;

import com.edutrack.backend.dto.CourseProgressResponse;
import com.edutrack.backend.security.CustomUserDetails;
import com.edutrack.backend.service.CourseProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseProgressController {
    private final CourseProgressService courseProgressService;

    @GetMapping("/{courseId}/progress")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<CourseProgressResponse> getCourseProgress(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID studentId = userDetails.getUser().getId();

        CourseProgressResponse response =
                courseProgressService.getCourseProgress(
                        courseId,
                        studentId
                );

        return ResponseEntity.ok(response);
    }
}
