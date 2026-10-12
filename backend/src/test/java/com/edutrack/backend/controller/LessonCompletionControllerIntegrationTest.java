package com.edutrack.backend.controller;

import com.edutrack.backend.entity.Course;
import com.edutrack.backend.entity.CourseModule;
import com.edutrack.backend.entity.CourseStatus;
import com.edutrack.backend.entity.Enrollment;
import com.edutrack.backend.entity.EnrollmentStatus;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LessonCompletionControllerIntegrationTest {

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
    private User student;
    private User secondStudent;
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

        student = createUser(
                "Test",
                "Student",
                "student@example.com",
                Role.STUDENT
        );

        secondStudent = createUser(
                "Second",
                "Student",
                "second.student@example.com",
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

    private Course createPublishedCourse() {
        Course course = new Course();
        course.setTitle("Java Fundamentals");
        course.setDescription("Learn the fundamentals of Java.");
        course.setInstructor(instructor);
        course.setStatus(CourseStatus.PUBLISHED);

        return courseRepository.save(course);
    }

    private CourseModule createModule(Course course) {
        CourseModule module = new CourseModule();
        module.setCourse(course);
        module.setTitle("Introduction to Java");
        module.setDisplayOrder(1);

        return courseModuleRepository.save(module);
    }

    private Lesson createLesson(CourseModule module) {
        Lesson lesson = new Lesson();
        lesson.setModule(module);
        lesson.setTitle("Variables and Data Types");
        lesson.setContent("Learn about Java variables and data types.");
        lesson.setDisplayOrder(1);

        return lessonRepository.save(lesson);
    }

    private Enrollment createActiveEnrollment(
            User student,
            Course course
    ) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        return enrollmentRepository.save(enrollment);
    }

    @Test
    void studentCanCompleteLesson() throws Exception {
        Course course = createPublishedCourse();
        CourseModule module = createModule(course);
        Lesson lesson = createLesson(module);

        createActiveEnrollment(student, course);

        String token = tokenFor(student);

        mockMvc.perform(post(
                        "/api/lesson-completions/" + lesson.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lessonId")
                        .value(lesson.getId().toString()))
                .andExpect(jsonPath("$.studentId")
                        .value(student.getId().toString()))
                .andExpect(jsonPath("$.completedAt")
                        .exists());
    }

    @Test
    void studentCanViewOwnCompletions() throws Exception {
        Course course = createPublishedCourse();
        CourseModule module = createModule(course);
        Lesson lesson = createLesson(module);

        createActiveEnrollment(student, course);

        String token = tokenFor(student);

        mockMvc.perform(post(
                        "/api/lesson-completions/" + lesson.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/lesson-completions/my-completions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].lessonId")
                        .value(lesson.getId().toString()))
                .andExpect(jsonPath("$[0].studentId")
                        .value(student.getId().toString()));
    }

    @Test
    void studentCannotCompleteLessonWithoutEnrollment() throws Exception {
        Course course = createPublishedCourse();
        CourseModule module = createModule(course);
        Lesson lesson = createLesson(module);

        String token = tokenFor(student);

        mockMvc.perform(post(
                        "/api/lesson-completions/" + lesson.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("You are not enrolled in this course"));
    }

    @Test
    void studentCannotCompleteSameLessonTwice() throws Exception {
        Course course = createPublishedCourse();
        CourseModule module = createModule(course);
        Lesson lesson = createLesson(module);

        createActiveEnrollment(student, course);

        String token = tokenFor(student);

        mockMvc.perform(post(
                        "/api/lesson-completions/" + lesson.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(post(
                        "/api/lesson-completions/" + lesson.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Lesson has already been completed"));
    }

    @Test
    void studentCannotCompleteLessonAfterEnrollmentIsDropped()
            throws Exception {

        Course course = createPublishedCourse();
        CourseModule module = createModule(course);
        Lesson lesson = createLesson(module);

        Enrollment enrollment =
                createActiveEnrollment(student, course);
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollmentRepository.save(enrollment);

        String token = tokenFor(student);

        mockMvc.perform(post(
                        "/api/lesson-completions/" + lesson.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Your enrollment is not active"));
    }

    @Test
    void studentCannotCompleteLessonAfterEnrollmentIsCompleted()
            throws Exception {

        Course course = createPublishedCourse();
        CourseModule module = createModule(course);
        Lesson lesson = createLesson(module);

        Enrollment enrollment =
                createActiveEnrollment(student, course);
        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        enrollmentRepository.save(enrollment);

        String token = tokenFor(student);

        mockMvc.perform(post(
                        "/api/lesson-completions/" + lesson.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Your enrollment is not active"));
    }

    @Test
    void instructorCannotCompleteLesson() throws Exception {
        Course course = createPublishedCourse();
        CourseModule module = createModule(course);
        Lesson lesson = createLesson(module);

        String token = tokenFor(instructor);

        mockMvc.perform(post(
                        "/api/lesson-completions/" + lesson.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotCompleteLesson() throws Exception {
        Course course = createPublishedCourse();
        CourseModule module = createModule(course);
        Lesson lesson = createLesson(module);

        String token = tokenFor(admin);

        mockMvc.perform(post(
                        "/api/lesson-completions/" + lesson.getId()
                )
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotCompleteLesson() throws Exception {
        Course course = createPublishedCourse();
        CourseModule module = createModule(course);
        Lesson lesson = createLesson(module);

        mockMvc.perform(post(
                        "/api/lesson-completions/" + lesson.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void completingNonexistentLessonReturnsNotFound() throws Exception {
        String token = tokenFor(student);

        mockMvc.perform(post(
                        "/api/lesson-completions/00000000-0000-0000-0000-000000000000"
                )
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Lesson not found"));
    }

    @Test
    void studentCannotViewCompletionsAsAnotherStudent() throws Exception {
        String token = tokenFor(secondStudent);

        mockMvc.perform(get("/api/lesson-completions/my-completions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unauthenticatedUserCannotViewCompletions() throws Exception {
        mockMvc.perform(get("/api/lesson-completions/my-completions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void instructorCannotViewCompletions() throws Exception {
        String token = tokenFor(instructor);

        mockMvc.perform(get("/api/lesson-completions/my-completions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotViewCompletions() throws Exception {
        String token = tokenFor(admin);

        mockMvc.perform(get("/api/lesson-completions/my-completions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}