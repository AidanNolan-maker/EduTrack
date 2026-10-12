package com.edutrack.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreateModuleRequest {
    @NotNull
    private UUID courseId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotNull
    private Integer displayOrder;

}
