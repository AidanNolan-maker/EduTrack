package com.edutrack.backend.controller;

import com.edutrack.backend.entity.Course;
import com.edutrack.backend.entity.CourseStatus;
import com.edutrack.backend.entity.Role;
import com.edutrack.backend.entity.User;
import com.edutrack.backend.repository.LessonCompletionRepository;
import com.edutrack.backend.repository.LessonRepository;
import com.edutrack.backend.repository.CourseRepository;
import com.edutrack.backend.repository.CourseModuleRepository;
import com.edutrack.backend.repository.UserRepository;
import com.edutrack.backend.repository.EnrollmentRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseUpdateControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LessonCompletionRepository lessonCompletionRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseModuleRepository courseModuleRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

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
        lessonCompletionRepository.deleteAll();
        lessonRepository.deleteAll();
        courseModuleRepository.deleteAll();
        enrollmentRepository.deleteAll();
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
            String description,
            CourseStatus status,
            User courseInstructor
    ) {
        Course course = new Course();
        course.setTitle(title);
        course.setDescription(description);
        course.setInstructor(courseInstructor);
        course.setStatus(status);

        return courseRepository.save(course);
    }

    @Test
    void instructorCanUpdateOwnCourse() throws Exception {
        Course course = createCourse(
                "Original Title",
                "Original description.",
                CourseStatus.DRAFT,
                instructor
        );

        String token = tokenFor(instructor);

        mockMvc.perform(put("/api/courses/" + course.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Updated Title",
                                    "description": "Updated description."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(course.getId().toString()))
                .andExpect(jsonPath("$.title")
                        .value("Updated Title"))
                .andExpect(jsonPath("$.description")
                        .value("Updated description."))
                .andExpect(jsonPath("$.courseStatus")
                        .value("DRAFT"))
                .andExpect(jsonPath("$.instructorId")
                        .value(instructor.getId().toString()));
    }

    @Test
    void adminCanUpdateAnyCourse() throws Exception {
        Course course = createCourse(
                "Original Title",
                "Original description.",
                CourseStatus.DRAFT,
                instructor
        );

        String token = tokenFor(admin);

        mockMvc.perform(put("/api/courses/" + course.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Admin Updated Title",
                                    "description": "Admin updated description."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title")
                        .value("Admin Updated Title"))
                .andExpect(jsonPath("$.description")
                        .value("Admin updated description."))
                .andExpect(jsonPath("$.courseStatus")
                        .value("DRAFT"))
                .andExpect(jsonPath("$.instructorId")
                        .value(instructor.getId().toString()));
    }

    @Test
    void instructorCannotUpdateAnotherInstructorsCourse()
            throws Exception {

        Course course = createCourse(
                "Original Title",
                "Original description.",
                CourseStatus.DRAFT,
                instructor
        );

        String token = tokenFor(secondInstructor);

        mockMvc.perform(put("/api/courses/" + course.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Unauthorized Update",
                                    "description": "This should not be allowed."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value(
                                "You do not have permission to update this course"
                        ));
    }

    @Test
    void studentCannotUpdateCourse() throws Exception {
        Course course = createCourse(
                "Original Title",
                "Original description.",
                CourseStatus.DRAFT,
                instructor
        );

        String token = tokenFor(student);

        mockMvc.perform(put("/api/courses/" + course.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Unauthorized Update",
                                    "description": "This should not be allowed."
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotUpdateCourse() throws Exception {
        Course course = createCourse(
                "Original Title",
                "Original description.",
                CourseStatus.DRAFT,
                instructor
        );

        mockMvc.perform(put("/api/courses/" + course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Unauthorized Update",
                                    "description": "This should not be allowed."
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatingNonexistentCourseReturnsNotFound()
            throws Exception {

        String token = tokenFor(instructor);

        mockMvc.perform(put(
                        "/api/courses/00000000-0000-0000-0000-000000000000"
                )
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Updated Title",
                                    "description": "Updated description."
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Course Not Found"));
    }

    @Test
    void blankTitleIsRejected() throws Exception {
        Course course = createCourse(
                "Original Title",
                "Original description.",
                CourseStatus.DRAFT,
                instructor
        );

        String token = tokenFor(instructor);

        mockMvc.perform(put("/api/courses/" + course.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "",
                                    "description": "Updated description."
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankDescriptionIsRejected() throws Exception {
        Course course = createCourse(
                "Original Title",
                "Original description.",
                CourseStatus.DRAFT,
                instructor
        );

        String token = tokenFor(instructor);

        mockMvc.perform(put("/api/courses/" + course.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Updated Title",
                                    "description": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void titleOverTwoHundredCharactersIsRejected()
            throws Exception {

        Course course = createCourse(
                "Original Title",
                "Original description.",
                CourseStatus.DRAFT,
                instructor
        );

        String token = tokenFor(instructor);

        String longTitle = "a".repeat(201);

        mockMvc.perform(put("/api/courses/" + course.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "%s",
                                    "description": "Updated description."
                                }
                                """.formatted(longTitle)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatingPublishedCoursePreservesPublishedStatus()
            throws Exception {

        Course course = createCourse(
                "Original Title",
                "Original description.",
                CourseStatus.PUBLISHED,
                instructor
        );

        String token = tokenFor(instructor);

        mockMvc.perform(put("/api/courses/" + course.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Updated Published Course",
                                    "description": "Updated description."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title")
                        .value("Updated Published Course"))
                .andExpect(jsonPath("$.description")
                        .value("Updated description."))
                .andExpect(jsonPath("$.courseStatus")
                        .value("PUBLISHED"));
    }

    @Test
    void updatingArchivedCoursePreservesArchivedStatus()
            throws Exception {

        Course course = createCourse(
                "Original Title",
                "Original description.",
                CourseStatus.ARCHIVED,
                instructor
        );

        String token = tokenFor(instructor);

        mockMvc.perform(put("/api/courses/" + course.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Updated Archived Course",
                                    "description": "Updated description."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title")
                        .value("Updated Archived Course"))
                .andExpect(jsonPath("$.description")
                        .value("Updated description."))
                .andExpect(jsonPath("$.courseStatus")
                        .value("ARCHIVED"));
    }
}