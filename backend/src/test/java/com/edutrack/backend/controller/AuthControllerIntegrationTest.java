package com.edutrack.backend.controller;

import com.edutrack.backend.entity.User;
import com.edutrack.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerReturnsCreatedUser() throws Exception {
        String request = """
                {
                    "firstName": "HTTP",
                    "lastName": "Test",
                    "email": "http.test@example.com",
                    "password": "password123"
                }
                """;

        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(nullValue())))
                .andExpect(jsonPath("$.firstName").value("HTTP"))
                .andExpect(jsonPath("$.lastName").value("Test"))
                .andExpect(jsonPath("$.email").value("http.test@example.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User user = userRepository.findByEmail("http.test@example.com")
                .orElseThrow();

        org.junit.jupiter.api.Assertions.assertTrue(
                passwordEncoder.matches("password123", user.getPassword())
        );
    }

    @Test
    void registerRejectsInvalidEmail() throws Exception {
        String request = """
                {
                    "firstName": "Invalid",
                    "lastName": "Email",
                    "email": "not-an-email",
                    "password": "password123"
                }
                """;

        mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerRejectsShortPassword() throws Exception {
        String request = """
                {
                    "firstName": "Short",
                    "lastName": "Password",
                    "email": "short.password@example.com",
                    "password": "short"
                }
                """;

        mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
        )
                .andExpect(status().isBadRequest());
    }
}
