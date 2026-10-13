package com.edutrack.backend.controller;

import com.edutrack.backend.dto.CreateLessonRequest;
import com.edutrack.backend.dto.UpdateLessonRequest;
import com.edutrack.backend.dto.LessonResponse;
import com.edutrack.backend.entity.Lesson;
import com.edutrack.backend.mapper.LessonMapper;
import com.edutrack.backend.security.CustomUserDetails;
import com.edutrack.backend.service.LessonService;
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
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {
    private final LessonService lessonService;
    private final LessonMapper lessonMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<LessonResponse> createLesson(
            @Valid @RequestBody CreateLessonRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUser().getId();

        Lesson lesson = lessonService.createLesson(
                request,
                userId
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(lessonMapper.toResponse(lesson));
    }

    @GetMapping("/module/{moduleId}")
    public ResponseEntity<List<LessonResponse>> getLessonsForModule(
            @PathVariable UUID moduleId
    ) {
        List<LessonResponse> lessons = lessonService
                .getLessonsForModule(moduleId)
                .stream()
                .map(lessonMapper::toResponse)
                .toList();

        return ResponseEntity.ok(lessons);
    }

    @PutMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<LessonResponse> updateLesson(
            @PathVariable UUID lessonId,
            @Valid @RequestBody UpdateLessonRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        LessonResponse response = lessonService.updateLesson(
                lessonId,
                request,
                userDetails.getUser().getId()
        );

        return ResponseEntity.ok(response);
    }
}
