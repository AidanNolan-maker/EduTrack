package com.edutrack.backend.service;

import com.edutrack.backend.dto.RegisterRequest;
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
public class AuthServiceIntegrationTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerCreatesStudentWithHashedPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Test");
        request.setLastName("Student");
        request.setEmail("test.student@example.com");
        request.setPassword("password123");

        User user = authService.register(request);

        assertNotNull(user.getId());
        assertEquals("Test", user.getFirstName());
        assertEquals("Student", user.getLastName());
        assertEquals("test.student@example.com",  user.getEmail());
        assertEquals(Role.STUDENT, user.getRole());
        assertTrue(user.isEnabled());

        assertNotEquals("password123", user.getPassword());
        assertTrue(passwordEncoder.matches("password123", user.getPassword()));

        assertTrue(userRepository.existsByEmail("test.student@example.com"));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest firstRequest = new RegisterRequest();
        firstRequest.setFirstName("Test");
        firstRequest.setLastName("Student");
        firstRequest.setEmail("duplicate@example.com");
        firstRequest.setPassword("password123");

        authService.register(firstRequest);

        RegisterRequest secondRequest = new RegisterRequest();
        secondRequest.setFirstName("Second");
        secondRequest.setLastName("Student");
        secondRequest.setEmail("duplicate@example.com");
        secondRequest.setPassword("password456");

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(secondRequest)
        );
    }
}
