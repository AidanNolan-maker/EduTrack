package com.edutrack.backend.controller;

import com.edutrack.backend.dto.CreateModuleRequest;
import com.edutrack.backend.dto.ModuleResponse;
import com.edutrack.backend.entity.CourseModule;
import com.edutrack.backend.mapper.CourseModuleMapper;
import com.edutrack.backend.security.CustomUserDetails;
import com.edutrack.backend.service.CourseModuleService;
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
@RequestMapping("/api/modules")
@RequiredArgsConstructor
public class CourseModuleController {
    private final CourseModuleService courseModuleService;
    private final CourseModuleMapper courseModuleMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ModuleResponse> createModule(
            @Valid @RequestBody CreateModuleRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = userDetails.getUser().getId();

        CourseModule courseModule = courseModuleService.createModule(
                request,
                userId
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(courseModuleMapper.toResponse(courseModule));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<ModuleResponse>> getModulesForCourse(
            @PathVariable UUID courseId
    ) {
        List<ModuleResponse> modules = courseModuleService
                .getModulesForCourse(courseId)
                .stream()
                .map(courseModuleMapper::toResponse)
                .toList();

        return ResponseEntity.ok(modules);
    }
}
