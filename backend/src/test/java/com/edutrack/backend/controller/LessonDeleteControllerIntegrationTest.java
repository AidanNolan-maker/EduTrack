package com.edutrack.backend.controller;

import com.edutrack.backend.entity.Course;
import com.edutrack.backend.entity.CourseModule;
import com.edutrack.backend.entity.CourseStatus;
import com.edutrack.backend.entity.Lesson;
import com.edutrack.backend.entity.LessonCompletion;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LessonDeleteControllerIntegrationTest {

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
    void instructorCanDeleteOwnLesson() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        CourseModule module = createModule(course);

        Lesson lesson = createLesson(
                module,
                "Lesson to Delete",
                "Lesson content.",
                1
        );

        String token = tokenFor(instructor);

        mockMvc.perform(
                        delete("/api/lessons/{lessonId}", lesson.getId())
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isNoContent());

        assertFalse(lessonRepository.existsById(lesson.getId()));
    }

    @Test
    void adminCanDeleteAnyLesson() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        course.setInstructor(secondInstructor);
        course = courseRepository.save(course);

        CourseModule module = createModule(course);

        Lesson lesson = createLesson(
                module,
                "Lesson to Delete",
                "Lesson content.",
                1
        );

        String token = tokenFor(admin);

        mockMvc.perform(
                        delete("/api/lessons/{lessonId}", lesson.getId())
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isNoContent());

        assertFalse(lessonRepository.existsById(lesson.getId()));
    }

    @Test
    void instructorCannotDeleteAnotherInstructorsLesson()
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
                        delete("/api/lessons/{lessonId}", lesson.getId())
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value(
                                "You do not have permission to delete this lesson"
                        ));

        assertTrue(lessonRepository.existsById(lesson.getId()));
    }

    @Test
    void studentCannotDeleteLesson() throws Exception {
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
                        delete("/api/lessons/{lessonId}", lesson.getId())
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isForbidden());

        assertTrue(lessonRepository.existsById(lesson.getId()));
    }

    @Test
    void unauthenticatedUserCannotDeleteLesson() throws Exception {
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
                        delete("/api/lessons/{lessonId}", lesson.getId())
                )
                .andExpect(status().isUnauthorized());

        assertTrue(lessonRepository.existsById(lesson.getId()));
    }

    @Test
    void deletingNonexistentLessonReturnsNotFound()
            throws Exception {

        String token = tokenFor(instructor);
        UUID lessonId = UUID.randomUUID();

        mockMvc.perform(
                        delete("/api/lessons/{lessonId}", lessonId)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Lesson Not Found"));
    }

    @Test
    void deletingLessonAlsoDeletesCompletionRecords()
            throws Exception {

        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        CourseModule module = createModule(course);

        Lesson lesson = createLesson(
                module,
                "Completed Lesson",
                "Lesson content.",
                1
        );

        LessonCompletion completion = new LessonCompletion();
        completion.setStudent(student);
        completion.setLesson(lesson);

        completion = lessonCompletionRepository.save(completion);

        UUID completionId = completion.getId();

        String token = tokenFor(instructor);

        mockMvc.perform(
                        delete("/api/lessons/{lessonId}", lesson.getId())
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isNoContent());

        assertFalse(lessonRepository.existsById(lesson.getId()));
        assertFalse(lessonCompletionRepository.existsById(completionId));
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