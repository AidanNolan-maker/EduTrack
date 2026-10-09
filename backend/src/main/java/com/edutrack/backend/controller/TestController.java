package com.edutrack.backend.controller;

import com.edutrack.backend.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestController {
    @GetMapping("/api/test/protected")
    public Map<String, Object> protectedEndpoint(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return Map.of(
                "message", "You are authenticated",
                "userId", userDetails.getUser().getId(),
                "email", userDetails.getUsername(),
                "role", userDetails.getUser().getRole()
        );
    }
}
