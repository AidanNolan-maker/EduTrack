package com.edutrack.backend.controller;

import com.edutrack.backend.entity.Course;
import com.edutrack.backend.entity.CourseModule;
import com.edutrack.backend.entity.CourseStatus;
import com.edutrack.backend.entity.Lesson;
import com.edutrack.backend.entity.Role;
import com.edutrack.backend.entity.User;
import com.edutrack.backend.repository.CourseModuleRepository;
import com.edutrack.backend.repository.CourseRepository;
import com.edutrack.backend.repository.EnrollmentRepository;
import com.edutrack.backend.repository.LessonCompletionRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LessonUpdateControllerIntegrationTest {

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
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private LessonCompletionRepository lessonCompletionRepository;

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

    @Test
    void instructorCanUpdateOwnLesson() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        CourseModule module = createModule(course);

        Lesson lesson = createLesson(
                module,
                "Old Lesson",
                "Old content.",
                1
        );

        String token = tokenFor(instructor);

        mockMvc.perform(
                        put("/api/lessons/{lessonId}", lesson.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "Updated Lesson",
                                            "content": "Updated content.",
                                            "displayOrder": 2
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(lesson.getId().toString()))
                .andExpect(jsonPath("$.moduleId")
                        .value(module.getId().toString()))
                .andExpect(jsonPath("$.courseId")
                        .value(course.getId().toString()))
                .andExpect(jsonPath("$.title")
                        .value("Updated Lesson"))
                .andExpect(jsonPath("$.content")
                        .value("Updated content."))
                .andExpect(jsonPath("$.displayOrder")
                        .value(2));
    }

    @Test
    void adminCanUpdateAnyLesson() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        course.setInstructor(secondInstructor);
        course = courseRepository.save(course);

        CourseModule module = createModule(course);

        Lesson lesson = createLesson(
                module,
                "Old Lesson",
                "Old content.",
                1
        );

        String token = tokenFor(admin);

        mockMvc.perform(
                        put("/api/lessons/{lessonId}", lesson.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "Admin Updated Lesson",
                                            "content": "Admin updated content.",
                                            "displayOrder": 3
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title")
                        .value("Admin Updated Lesson"))
                .andExpect(jsonPath("$.content")
                        .value("Admin updated content."))
                .andExpect(jsonPath("$.displayOrder")
                        .value(3));
    }

    @Test
    void instructorCannotUpdateAnotherInstructorsLesson()
            throws Exception {

        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        course.setInstructor(secondInstructor);
        course = courseRepository.save(course);

        CourseModule module = createModule(course);

        Lesson lesson = createLesson(
                module,
                "Lesson",
                "Lesson content.",
                1
        );

        String token = tokenFor(instructor);

        mockMvc.perform(
                        put("/api/lessons/{lessonId}", lesson.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "Unauthorized Lesson",
                                            "content": "Unauthorized content.",
                                            "displayOrder": 2
                                        }
                                        """)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value(
                                "You do not have permission to update this lesson"
                        ));
    }

    @Test
    void studentCannotUpdateLesson() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        CourseModule module = createModule(course);

        Lesson lesson = createLesson(
                module,
                "Lesson",
                "Lesson content.",
                1
        );

        String token = tokenFor(student);

        mockMvc.perform(
                        put("/api/lessons/{lessonId}", lesson.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "Unauthorized Lesson",
                                            "content": "Unauthorized content.",
                                            "displayOrder": 2
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotUpdateLesson() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        CourseModule module = createModule(course);

        Lesson lesson = createLesson(
                module,
                "Lesson",
                "Lesson content.",
                1
        );

        mockMvc.perform(
                        put("/api/lessons/{lessonId}", lesson.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "Unauthorized Lesson",
                                            "content": "Unauthorized content.",
                                            "displayOrder": 2
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatingNonexistentLessonReturnsNotFound() throws Exception {
        String token = tokenFor(instructor);

        mockMvc.perform(
                        put("/api/lessons/{lessonId}", UUID.randomUUID())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "Updated Lesson",
                                            "content": "Updated content.",
                                            "displayOrder": 2
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Lesson Not Found"));
    }

    @Test
    void blankTitleIsRejected() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        CourseModule module = createModule(course);

        Lesson lesson = createLesson(
                module,
                "Lesson",
                "Lesson content.",
                1
        );

        String token = tokenFor(instructor);

        mockMvc.perform(
                        put("/api/lessons/{lessonId}", lesson.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "",
                                            "content": "Updated content.",
                                            "displayOrder": 2
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankContentIsRejected() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        CourseModule module = createModule(course);

        Lesson lesson = createLesson(
                module,
                "Lesson",
                "Lesson content.",
                1
        );

        String token = tokenFor(instructor);

        mockMvc.perform(
                        put("/api/lessons/{lessonId}", lesson.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "Updated Lesson",
                                            "content": "",
                                            "displayOrder": 2
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void titleOverTwoHundredCharactersIsRejected() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        CourseModule module = createModule(course);

        Lesson lesson = createLesson(
                module,
                "Lesson",
                "Lesson content.",
                1
        );

        String token = tokenFor(instructor);
        String title = "a".repeat(201);

        mockMvc.perform(
                        put("/api/lessons/{lessonId}", lesson.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "%s",
                                            "content": "Updated content.",
                                            "displayOrder": 2
                                        }
                                        """.formatted(title))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingDisplayOrderIsRejected() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        CourseModule module = createModule(course);

        Lesson lesson = createLesson(
                module,
                "Lesson",
                "Lesson content.",
                1
        );

        String token = tokenFor(instructor);

        mockMvc.perform(
                        put("/api/lessons/{lessonId}", lesson.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "Updated Lesson",
                                            "content": "Updated content."
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatedLessonIsPersisted() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        CourseModule module = createModule(course);

        Lesson lesson = createLesson(
                module,
                "Old Lesson",
                "Old content.",
                1
        );

        String token = tokenFor(instructor);

        mockMvc.perform(
                        put("/api/lessons/{lessonId}", lesson.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "Persisted Lesson",
                                            "content": "Persisted content.",
                                            "displayOrder": 5
                                        }
                                        """)
                )
                .andExpect(status().isOk());

        Lesson updatedLesson = lessonRepository
                .findById(lesson.getId())
                .orElseThrow();

        assertEquals(
                "Persisted Lesson",
                updatedLesson.getTitle()
        );

        assertEquals(
                "Persisted content.",
                updatedLesson.getContent()
        );

        assertEquals(
                5,
                updatedLesson.getDisplayOrder()
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
            CourseStatus status
    ) {
        Course course = new Course();
        course.setTitle(title);
        course.setDescription("Test course description.");
        course.setInstructor(instructor);
        course.setStatus(status);

        return courseRepository.save(course);
    }

    private CourseModule createModule(Course course) {
        CourseModule module = new CourseModule();
        module.setCourse(course);
        module.setTitle("Introduction to Java");
        module.setDisplayOrder(1);

        return courseModuleRepository.save(module);
    }

    private Lesson createLesson(
            CourseModule module,
            String title,
            String content,
            int displayOrder
    ) {
        Lesson lesson = new Lesson();
        lesson.setModule(module);
        lesson.setTitle(title);
        lesson.setContent(content);
        lesson.setDisplayOrder(displayOrder);

        return lessonRepository.save(lesson);
    }
}