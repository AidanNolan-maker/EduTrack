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

import com.edutrack.backend.entity.Role;
import com.edutrack.backend.entity.User;

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

    @Test
    void loginReturnsJwtForValidCredentials() throws Exception {
        User user = new User();
        user.setFirstName("Login");
        user.setLastName("Test");
        user.setEmail("http.login@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(Role.STUDENT);
        user.setEnabled(true);

        userRepository.save(user);

        String request = """
                {
                    "email": "http.login@example.com",
                    "password": "password123"
                }
                """;

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.firstName").value("Login"))
                .andExpect(jsonPath("$.lastName").value("Test"))
                .andExpect(jsonPath("$.email").value("http.login@example.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void loginRejectsIncorrectPassword() throws Exception {
        User user = new User();
        user.setFirstName("Wrong");
        user.setLastName("Password");
        user.setEmail("http.wrong@example.com");
        user.setPassword(passwordEncoder.encode("correct-password"));
        user.setRole(Role.STUDENT);
        user.setEnabled(true);

        userRepository.save(user);

        String request = """
            {
                "email": "http.wrong@example.com",
                "password": "wrong-password"
            }
            """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRejectsDisabledAccount() throws Exception {
        User user = new User();
        user.setFirstName("Disabled");
        user.setLastName("HTTP");
        user.setEmail("http.disabled@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(Role.STUDENT);
        user.setEnabled(false);

        userRepository.save(user);

        String request = """
            {
                "email": "http.disabled@example.com",
                "password": "password123"
            }
            """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Account is disabled"));
    }
}
