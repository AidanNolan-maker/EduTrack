package com.edutrack.backend.controller;

import com.edutrack.backend.dto.LessonCompletionResponse;
import com.edutrack.backend.entity.LessonCompletion;
import com.edutrack.backend.security.CustomUserDetails;
import com.edutrack.backend.service.LessonCompletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lesson-completions")
@RequiredArgsConstructor
public class LessonCompletionController {
    private final LessonCompletionService lessonCompletionService;

    @PostMapping("/{lessonId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<LessonCompletionResponse> completeLesson(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID studentId = userDetails.getUser().getId();

        LessonCompletion completion =
                lessonCompletionService.completeLesson(
                        lessonId,
                        studentId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(completion));
    }

    @GetMapping("/my-completions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<LessonCompletionResponse>> getMyCompletions(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID studentId = userDetails.getUser().getId();

        List<LessonCompletionResponse> completions =
                lessonCompletionService
                        .getStudentCompletions(studentId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(completions);
    }

    private LessonCompletionResponse toResponse(
            LessonCompletion completion
    ) {
        LessonCompletionResponse response =
                new LessonCompletionResponse();

        response.setId(completion.getId());
        response.setLessonId(completion.getLesson().getId());
        response.setStudentId(completion.getStudent().getId());
        response.setCompletedAt(completion.getCompletedAt());

        return response;
    }
}
