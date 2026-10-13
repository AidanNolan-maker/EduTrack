package com.edutrack.backend.controller;

import com.edutrack.backend.entity.Course;
import com.edutrack.backend.entity.CourseModule;
import com.edutrack.backend.entity.CourseStatus;
import com.edutrack.backend.entity.Role;
import com.edutrack.backend.entity.User;
import com.edutrack.backend.repository.CourseModuleRepository;
import com.edutrack.backend.repository.CourseRepository;
import com.edutrack.backend.repository.LessonRepository;
import com.edutrack.backend.repository.UserRepository;
import com.edutrack.backend.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseModuleUpdateControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseModuleRepository courseModuleRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User instructor;
    private User secondInstructor;
    private User student;
    private User admin;

    @BeforeEach
    void setUp() {
        lessonRepository.deleteAll();
        courseModuleRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        instructor = createUser(
                "Test",
                "Instructor",
                "instructor@example.com",
                Role.INSTRUCTOR
        );

        secondInstructor = createUser(
                "Second",
                "Instructor",
                "second.instructor@example.com",
                Role.INSTRUCTOR
        );

        student = createUser(
                "Test",
                "Student",
                "student@example.com",
                Role.STUDENT
        );

        admin = createUser(
                "Test",
                "Admin",
                "admin@example.com",
                Role.ADMIN
        );
    }

    private User createUser(
            String firstName,
            String lastName,
            String email,
            Role role
    ) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(role);
        user.setEnabled(true);

        return userRepository.save(user);
    }

    private String tokenFor(User user) {
        return jwtService.generateToken(user);
    }

    private Course createCourse(
            String title,
            CourseStatus status,
            User courseInstructor
    ) {
        Course course = new Course();
        course.setTitle(title);
        course.setDescription("Test course description.");
        course.setInstructor(courseInstructor);
        course.setStatus(status);

        return courseRepository.save(course);
    }

    private CourseModule createModule(
            Course course,
            String title,
            int displayOrder
    ) {
        CourseModule module = new CourseModule();
        module.setCourse(course);
        module.setTitle(title);
        module.setDisplayOrder(displayOrder);

        return courseModuleRepository.save(module);
    }

    @Test
    void instructorCanUpdateOwnModule() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT,
                instructor
        );

        CourseModule module = createModule(
                course,
                "Introduction to Java",
                1
        );

        mockMvc.perform(put("/api/modules/{moduleId}", module.getId())
                        .header(
                                "Authorization",
                                "Bearer " + tokenFor(instructor)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Updated Introduction to Java",
                                    "displayOrder": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(module.getId().toString()))
                .andExpect(jsonPath("$.courseId")
                        .value(course.getId().toString()))
                .andExpect(jsonPath("$.title")
                        .value("Updated Introduction to Java"))
                .andExpect(jsonPath("$.displayOrder")
                        .value(2));
    }

    @Test
    void adminCanUpdateAnyModule() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT,
                instructor
        );

        CourseModule module = createModule(
                course,
                "Introduction to Java",
                1
        );

        mockMvc.perform(put("/api/modules/{moduleId}", module.getId())
                        .header(
                                "Authorization",
                                "Bearer " + tokenFor(admin)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Admin Updated Module",
                                    "displayOrder": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title")
                        .value("Admin Updated Module"))
                .andExpect(jsonPath("$.displayOrder")
                        .value(3));
    }

    @Test
    void instructorCannotUpdateAnotherInstructorsModule() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT,
                instructor
        );

        CourseModule module = createModule(
                course,
                "Introduction to Java",
                1
        );

        mockMvc.perform(put("/api/modules/{moduleId}", module.getId())
                        .header(
                                "Authorization",
                                "Bearer " + tokenFor(secondInstructor)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Unauthorized Update",
                                    "displayOrder": 2
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value(
                                "You do not have permission to update this module"
                        ));

        CourseModule unchanged = courseModuleRepository
                .findById(module.getId())
                .orElseThrow();

        assertThat(unchanged.getTitle())
                .isEqualTo("Introduction to Java");
        assertThat(unchanged.getDisplayOrder())
                .isEqualTo(1);
    }

    @Test
    void studentCannotUpdateModule() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT,
                instructor
        );

        CourseModule module = createModule(
                course,
                "Introduction to Java",
                1
        );

        mockMvc.perform(put("/api/modules/{moduleId}", module.getId())
                        .header(
                                "Authorization",
                                "Bearer " + tokenFor(student)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Unauthorized Update",
                                    "displayOrder": 2
                                }
                                """))
                .andExpect(status().isForbidden());

        assertThat(courseModuleRepository.findById(module.getId()))
                .isPresent();
    }

    @Test
    void unauthenticatedUserCannotUpdateModule() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT,
                instructor
        );

        CourseModule module = createModule(
                course,
                "Introduction to Java",
                1
        );

        mockMvc.perform(put("/api/modules/{moduleId}", module.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Unauthorized Update",
                                    "displayOrder": 2
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatingNonexistentModuleReturnsNotFound() throws Exception {
        UUID moduleId = UUID.randomUUID();

        mockMvc.perform(put("/api/modules/{moduleId}", moduleId)
                        .header(
                                "Authorization",
                                "Bearer " + tokenFor(instructor)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Updated Module",
                                    "displayOrder": 2
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void blankTitleIsRejected() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT,
                instructor
        );

        CourseModule module = createModule(
                course,
                "Introduction to Java",
                1
        );

        mockMvc.perform(put("/api/modules/{moduleId}", module.getId())
                        .header(
                                "Authorization",
                                "Bearer " + tokenFor(instructor)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "",
                                    "displayOrder": 2
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void titleOverTwoHundredCharactersIsRejected() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT,
                instructor
        );

        CourseModule module = createModule(
                course,
                "Introduction to Java",
                1
        );

        String title = "a".repeat(201);

        mockMvc.perform(put("/api/modules/{moduleId}", module.getId())
                        .header(
                                "Authorization",
                                "Bearer " + tokenFor(instructor)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "%s",
                                    "displayOrder": 2
                                }
                                """.formatted(title)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingDisplayOrderIsRejected() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT,
                instructor
        );

        CourseModule module = createModule(
                course,
                "Introduction to Java",
                1
        );

        mockMvc.perform(put("/api/modules/{moduleId}", module.getId())
                        .header(
                                "Authorization",
                                "Bearer " + tokenFor(instructor)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Updated Module"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatedModuleIsPersisted() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT,
                instructor
        );

        CourseModule module = createModule(
                course,
                "Introduction to Java",
                1
        );

        mockMvc.perform(put("/api/modules/{moduleId}", module.getId())
                        .header(
                                "Authorization",
                                "Bearer " + tokenFor(instructor)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Updated Module",
                                    "displayOrder": 5
                                }
                                """))
                .andExpect(status().isOk());

        CourseModule updated = courseModuleRepository
                .findById(module.getId())
                .orElseThrow();

        assertThat(updated.getTitle())
                .isEqualTo("Updated Module");
        assertThat(updated.getDisplayOrder())
                .isEqualTo(5);
        assertThat(updated.getCourse().getId())
                .isEqualTo(course.getId());
    }
}