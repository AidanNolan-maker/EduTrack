package com.edutrack.backend.service;

import com.edutrack.backend.dto.LoginRequest;
import com.edutrack.backend.dto.LoginResponse;
import com.edutrack.backend.entity.Role;
import com.edutrack.backend.entity.User;
import com.edutrack.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class AuthLoginIntegrationTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loginReturnsJwtForValidCredentials() {
        User user = new User();
        user.setFirstName("Login");
        user.setLastName("Test");
        user.setEmail("login.test@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(Role.STUDENT);
        user.setEnabled(true);

        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setEmail("login.test@example.com");
        request.setPassword("password123");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertFalse(response.getToken().isBlank());

        assertEquals(user.getId(), response.getUserId());
        assertEquals("Login", response.getFirstName());
        assertEquals("Test", response.getLastName());
        assertEquals("login.test@example.com", response.getEmail());
        assertEquals(Role.STUDENT, response.getRole());
    }

    @Test
    void loginRejectsIncorrectPassword() {
        User user = new User();
        user.setFirstName("Wrong");
        user.setLastName("Password");
        user.setEmail("wrong.password@example.com");
        user.setPassword(passwordEncoder.encode("correct-password"));
        user.setRole(Role.STUDENT);
        user.setEnabled(true);

        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setEmail("wrong.password@example.com");
        request.setPassword("wrong-password");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void loginRejectsUnknownEmail() {
        LoginRequest request = new LoginRequest();
        request.setEmail("does.not.exist@example.com");
        request.setPassword("password123");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void loginRejectsDisabledAccount() {
        User user = new User();
        user.setFirstName("Disabled");
        user.setLastName("User");
        user.setEmail("disabled@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(Role.STUDENT);
        user.setEnabled(false);

        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setEmail("disabled@example.com");
        request.setPassword("password123");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals("Account is disabled", exception.getMessage());
    }
}
