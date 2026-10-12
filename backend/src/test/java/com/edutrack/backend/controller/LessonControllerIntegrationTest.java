package com.edutrack.backend.controller;

import com.edutrack.backend.entity.Course;
import com.edutrack.backend.entity.CourseModule;
import com.edutrack.backend.entity.CourseStatus;
import com.edutrack.backend.entity.Lesson;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LessonControllerIntegrationTest {

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

    @Test
    void instructorCanCreateLessonForOwnCourse() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );
        CourseModule module = createModule(course);

        String token = tokenFor(instructor);

        mockMvc.perform(post("/api/lessons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "moduleId": "%s",
                                    "title": "Variables and Data Types",
                                    "content": "Learn about Java variables and data types.",
                                    "displayOrder": 1
                                }
                                """.formatted(module.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleId")
                        .value(module.getId().toString()))
                .andExpect(jsonPath("$.courseId")
                        .value(course.getId().toString()))
                .andExpect(jsonPath("$.title")
                        .value("Variables and Data Types"))
                .andExpect(jsonPath("$.content")
                        .value("Learn about Java variables and data types."))
                .andExpect(jsonPath("$.displayOrder")
                        .value(1));
    }

    @Test
    void adminCanCreateLessonForAnyCourse() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );
        CourseModule module = createModule(course);

        String token = tokenFor(admin);

        mockMvc.perform(post("/api/lessons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "moduleId": "%s",
                                    "title": "Variables and Data Types",
                                    "content": "Learn about Java variables and data types.",
                                    "displayOrder": 1
                                }
                                """.formatted(module.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleId")
                        .value(module.getId().toString()));
    }

    @Test
    void instructorCannotCreateLessonForAnotherInstructorsCourse()
            throws Exception {

        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );
        CourseModule module = createModule(course);

        String token = tokenFor(secondInstructor);

        mockMvc.perform(post("/api/lessons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "moduleId": "%s",
                                    "title": "Unauthorized Lesson",
                                    "content": "This should not be allowed.",
                                    "displayOrder": 1
                                }
                                """.formatted(module.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value(
                                "You do not have permission to manage this course"
                        ));
    }

    @Test
    void studentCannotCreateLesson() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );
        CourseModule module = createModule(course);

        String token = tokenFor(student);

        mockMvc.perform(post("/api/lessons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "moduleId": "%s",
                                    "title": "Unauthorized Lesson",
                                    "content": "This should not be allowed.",
                                    "displayOrder": 1
                                }
                                """.formatted(module.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotCreateLesson() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );
        CourseModule module = createModule(course);

        mockMvc.perform(post("/api/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "moduleId": "%s",
                                    "title": "Unauthorized Lesson",
                                    "content": "This should not be allowed.",
                                    "displayOrder": 1
                                }
                                """.formatted(module.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publishedCourseLessonsArePubliclyVisible() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.PUBLISHED
        );
        CourseModule module = createModule(course);

        Lesson lesson = createLesson(
                module,
                "Variables and Data Types",
                "Learn about Java variables and data types.",
                1
        );

        mockMvc.perform(get("/api/lessons/module/" + module.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(lesson.getId().toString()))
                .andExpect(jsonPath("$[0].moduleId")
                        .value(module.getId().toString()))
                .andExpect(jsonPath("$[0].courseId")
                        .value(course.getId().toString()))
                .andExpect(jsonPath("$[0].title")
                        .value("Variables and Data Types"))
                .andExpect(jsonPath("$[0].content")
                        .value("Learn about Java variables and data types."))
                .andExpect(jsonPath("$[0].displayOrder")
                        .value(1));
    }

    @Test
    void draftCourseLessonsAreNotPubliclyVisible() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );
        CourseModule module = createModule(course);

        createLesson(
                module,
                "Variables and Data Types",
                "Learn about Java variables and data types.",
                1
        );

        mockMvc.perform(get("/api/lessons/module/" + module.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value("Course content is not publicly available"));
    }

    @Test
    void archivedCourseLessonsAreNotPubliclyVisible() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.ARCHIVED
        );
        CourseModule module = createModule(course);

        createLesson(
                module,
                "Variables and Data Types",
                "Learn about Java variables and data types.",
                1
        );

        mockMvc.perform(get("/api/lessons/module/" + module.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value("Course content is not publicly available"));
    }

    @Test
    void lessonsAreReturnedInDisplayOrder() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.PUBLISHED
        );
        CourseModule module = createModule(course);

        createLesson(
                module,
                "Advanced Topics",
                "Advanced Java topics.",
                3
        );

        createLesson(
                module,
                "Variables",
                "Java variables.",
                1
        );

        createLesson(
                module,
                "Methods",
                "Java methods.",
                2
        );

        mockMvc.perform(get("/api/lessons/module/" + module.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title")
                        .value("Variables"))
                .andExpect(jsonPath("$[0].displayOrder")
                        .value(1))
                .andExpect(jsonPath("$[1].title")
                        .value("Methods"))
                .andExpect(jsonPath("$[1].displayOrder")
                        .value(2))
                .andExpect(jsonPath("$[2].title")
                        .value("Advanced Topics"))
                .andExpect(jsonPath("$[2].displayOrder")
                        .value(3));
    }
}