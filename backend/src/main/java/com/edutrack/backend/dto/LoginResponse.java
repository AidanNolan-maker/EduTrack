package com.edutrack.backend.dto;

import com.edutrack.backend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
}
