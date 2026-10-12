package com.edutrack.backend.controller;

import com.edutrack.backend.entity.Course;
import com.edutrack.backend.entity.CourseModule;
import com.edutrack.backend.entity.CourseStatus;
import com.edutrack.backend.entity.Role;
import com.edutrack.backend.entity.User;
import com.edutrack.backend.repository.CourseModuleRepository;
import com.edutrack.backend.repository.CourseRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseModuleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseModuleRepository courseModuleRepository;

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

    @Test
    void instructorCanCreateModuleForOwnCourse() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        String token = tokenFor(instructor);

        mockMvc.perform(post("/api/modules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "courseId": "%s",
                                    "title": "Introduction to Java",
                                    "displayOrder": 1
                                }
                                """.formatted(course.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId")
                        .value(course.getId().toString()))
                .andExpect(jsonPath("$.title")
                        .value("Introduction to Java"))
                .andExpect(jsonPath("$.displayOrder")
                        .value(1));
    }

    @Test
    void adminCanCreateModuleForAnyCourse() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        String token = tokenFor(admin);

        mockMvc.perform(post("/api/modules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "courseId": "%s",
                                    "title": "Introduction to Java",
                                    "displayOrder": 1
                                }
                                """.formatted(course.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId")
                        .value(course.getId().toString()));
    }

    @Test
    void instructorCannotCreateModuleForAnotherInstructorsCourse()
            throws Exception {

        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        String token = tokenFor(secondInstructor);

        mockMvc.perform(post("/api/modules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "courseId": "%s",
                                    "title": "Unauthorized Module",
                                    "displayOrder": 1
                                }
                                """.formatted(course.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value(
                                "You do not have permission to manage this course"
                        ));
    }

    @Test
    void studentCannotCreateModule() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        String token = tokenFor(student);

        mockMvc.perform(post("/api/modules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "courseId": "%s",
                                    "title": "Unauthorized Module",
                                    "displayOrder": 1
                                }
                                """.formatted(course.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotCreateModule() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        mockMvc.perform(post("/api/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "courseId": "%s",
                                    "title": "Unauthorized Module",
                                    "displayOrder": 1
                                }
                                """.formatted(course.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publishedCourseModulesArePubliclyVisible() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.PUBLISHED
        );

        CourseModule module = createModule(
                course,
                "Introduction to Java",
                1
        );

        mockMvc.perform(get("/api/modules/course/" + course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(module.getId().toString()))
                .andExpect(jsonPath("$[0].courseId")
                        .value(course.getId().toString()))
                .andExpect(jsonPath("$[0].title")
                        .value("Introduction to Java"))
                .andExpect(jsonPath("$[0].displayOrder")
                        .value(1));
    }

    @Test
    void draftCourseModulesAreNotPubliclyVisible() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.DRAFT
        );

        createModule(course, "Introduction to Java", 1);

        mockMvc.perform(get("/api/modules/course/" + course.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value("Course content is not publicly available"));
    }

    @Test
    void archivedCourseModulesAreNotPubliclyVisible() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.ARCHIVED
        );

        createModule(course, "Introduction to Java", 1);

        mockMvc.perform(get("/api/modules/course/" + course.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value("Course content is not publicly available"));
    }

    @Test
    void modulesAreReturnedInDisplayOrder() throws Exception {
        Course course = createCourse(
                "Java Fundamentals",
                CourseStatus.PUBLISHED
        );

        createModule(course, "Advanced Java", 3);
        createModule(course, "Introduction to Java", 1);
        createModule(course, "Object-Oriented Programming", 2);

        mockMvc.perform(get("/api/modules/course/" + course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title")
                        .value("Introduction to Java"))
                .andExpect(jsonPath("$[0].displayOrder")
                        .value(1))
                .andExpect(jsonPath("$[1].title")
                        .value("Object-Oriented Programming"))
                .andExpect(jsonPath("$[1].displayOrder")
                        .value(2))
                .andExpect(jsonPath("$[2].title")
                        .value("Advanced Java"))
                .andExpect(jsonPath("$[2].displayOrder")
                        .value(3));
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
}