package com.edutrack.backend.controller;

import com.edutrack.backend.entity.CourseStatus;
import com.edutrack.backend.entity.Role;
import com.edutrack.backend.entity.User;
import com.edutrack.backend.repository.CourseRepository;
import com.edutrack.backend.repository.UserRepository;
import com.edutrack.backend.service.JwtService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User instructor;
    private User secondInstructor;
    private User student;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
        userRepository.deleteAll();

        admin = createUser(
                "Admin",
                "Test",
                "admin@example.com",
                Role.ADMIN
        );

        instructor = createUser(
                "Instructor",
                "Test",
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
                "Student",
                "Test",
                "student@example.com",
                Role.STUDENT
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

    private String createCourse(User user, String title) throws Exception {
        String token = tokenFor(user);

        MvcResult result = mockMvc.perform(post("/api/courses")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "%s",
                                        "description": "Test course description."
                                    }
                                    """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();

        return JsonPath
                .parse(result.getResponse().getContentAsString())
                .read("$.id", String.class);
    }

    private String tokenFor(User user) {
        return jwtService.generateToken(user);
    }

    @Test
    void createCourseWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Introduction to Java",
                                    "description": "Learn Java fundamentals."
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotCreateCourse() throws Exception {
        String token = tokenFor(student);

        mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Introduction to Java",
                                    "description": "Learn Java fundamentals."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value("You do not have permission to perform this action"));
    }

    @Test
    void instructorCanCreateCourse() throws Exception {
        String token = tokenFor(instructor);

        mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Introduction to Java",
                                    "description": "Learn Java fundamentals."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title")
                        .value("Introduction to Java"))
                .andExpect(jsonPath("$.description")
                        .value("Learn Java fundamentals."))
                .andExpect(jsonPath("$.instructorId")
                        .value(instructor.getId().toString()))
                .andExpect(jsonPath("$.instructorFirstName")
                        .value("Instructor"))
                .andExpect(jsonPath("$.instructorLastName")
                        .value("Test"))
                .andExpect(jsonPath("$.courseStatus")
                        .value(CourseStatus.DRAFT.name()));
    }

    @Test
    void publishedCoursesOnlyReturnsPublishedCourses() throws Exception {
        String token = tokenFor(instructor);

        mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Draft Course",
                                    "description": "This course is still a draft."
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/courses/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void myCoursesReturnsInstructorCourses() throws Exception {
        String token = tokenFor(instructor);

        mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "My First Course",
                                    "description": "My first course."
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/courses/my-courses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title")
                        .value("My First Course"))
                .andExpect(jsonPath("$[0].instructorId")
                        .value(instructor.getId().toString()));
    }

    @Test
    void instructorCanPublishOwnCourse() throws Exception {
        String courseId = createCourse(
                instructor,
                "Java Fundamentals"
        );

        String token = tokenFor(instructor);

        mockMvc.perform(patch("/api/courses/" + courseId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseId))
                .andExpect(jsonPath("$.courseStatus")
                        .value(CourseStatus.PUBLISHED.name()));
    }

    @Test
    void instructorCannotPublishAnotherInstructorsCourse() throws Exception {
        String courseId = createCourse(instructor, "Java Fundamentals");
        String token = tokenFor(secondInstructor);

        mockMvc.perform(patch("/api/courses/" + courseId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value("You do not have permission to publish this course"));
    }

    @Test
    void studentCannotPublishCourse() throws Exception {
        String courseId = createCourse(instructor, "Java Fundamentals");
        String token = tokenFor(student);

        mockMvc.perform(patch("/api/courses/" + courseId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanPublishCourse() throws Exception {
        String courseId = createCourse(instructor, "Java Fundamentals");
        String token = tokenFor(admin);

        mockMvc.perform(patch("/api/courses/" + courseId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseStatus")
                        .value(CourseStatus.PUBLISHED.name()));
    }

    @Test
    void publishedCourseCannotBePublishedAgain() throws Exception {
        String courseId = createCourse(instructor, "Java Fundamentals");
        String token = tokenFor(instructor);

        mockMvc.perform(patch("/api/courses/" + courseId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/courses/" + courseId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Only draft courses can be published"));
    }
}
