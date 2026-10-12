package com.edutrack.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class ModuleResponse {
    private UUID id;
    private UUID courseId;
    private String title;
    private Integer displayOrder;
}
