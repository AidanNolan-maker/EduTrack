package com.edutrack.backend.controller;

import com.edutrack.backend.dto.EnrollmentRequest;
import com.edutrack.backend.dto.EnrollmentResponse;
import com.edutrack.backend.entity.Enrollment;
import com.edutrack.backend.mapper.EnrollmentMapper;
import com.edutrack.backend.security.CustomUserDetails;
import com.edutrack.backend.service.EnrollmentService;
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
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {
    private final EnrollmentService enrollmentService;
    private final EnrollmentMapper enrollmentMapper;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EnrollmentResponse> enrollStudent(
            @Valid @RequestBody EnrollmentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID studentId = userDetails.getUser().getId();

        Enrollment enrollment = enrollmentService.enrollStudent(
                request,
                studentId
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(enrollmentMapper.toResponse(enrollment));
    }

    @GetMapping("/my-enrollments")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<EnrollmentResponse>> getEnrollments(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID studentId = userDetails.getUser().getId();

        List<EnrollmentResponse> enrollments = enrollmentService
                .getStudentEnrollments(studentId)
                .stream()
                .map(enrollmentMapper::toResponse)
                .toList();

        return ResponseEntity.ok(enrollments);
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<EnrollmentResponse>> getCourseEnrollments(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUser().getId();

        List<EnrollmentResponse> enrollments = enrollmentService
                .getCourseEnrollments(courseId, userId)
                .stream()
                .map(enrollmentMapper::toResponse)
                .toList();

        return ResponseEntity.ok(enrollments);
    }

    @PatchMapping("/{enrollmentId}/drop")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EnrollmentResponse> dropEnrollment(
            @PathVariable UUID enrollmentId
    ) {
        Enrollment enrollment = enrollmentService.dropEnrollment(
                enrollmentId
        );

        return ResponseEntity.ok(enrollmentMapper.toResponse(enrollment));
    }

    @PatchMapping("/{enrollmentId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EnrollmentResponse> completeEnrollment(
            @PathVariable UUID enrollmentId
    ) {
        Enrollment enrollment = enrollmentService.completeEnrollment(
                enrollmentId
        );

        return ResponseEntity.ok(enrollmentMapper.toResponse(enrollment));
    }
}
