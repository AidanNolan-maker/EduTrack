package com.edutrack.backend.controller;

import com.edutrack.backend.entity.Role;
import com.edutrack.backend.entity.User;
import com.edutrack.backend.repository.UserRepository;
import com.edutrack.backend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void protectedEndpointRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(
                        get("/api/test/protected")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsInvalidToken() throws Exception {
        mockMvc.perform(
                        get("/api/test/protected")
                                .header("Authorization", "Bearer invalid-token")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointAcceptsValidToken() throws Exception {
        User user = new User();
        user.setFirstName("Protected");
        user.setLastName("User");
        user.setEmail("protected@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(Role.STUDENT);
        user.setEnabled(true);

        userRepository.save(user);

        String token = jwtService.generateToken(user);

        mockMvc.perform(
                        get("/api/test/protected")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("You are authenticated"))
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value("protected@example.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }
}