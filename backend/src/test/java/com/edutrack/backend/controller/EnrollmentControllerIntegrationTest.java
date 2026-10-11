package com.edutrack.backend.controller;

import com.edutrack.backend.entity.Course;
import com.edutrack.backend.entity.CourseStatus;
import com.edutrack.backend.entity.Enrollment;
import com.edutrack.backend.entity.EnrollmentStatus;
import com.edutrack.backend.entity.Role;
import com.edutrack.backend.entity.User;
import com.edutrack.backend.repository.CourseRepository;
import com.edutrack.backend.repository.EnrollmentRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnrollmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User student;
    private User secondStudent;
    private User instructor;
    private User admin;

    @BeforeEach
    void setUp() {
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

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

        instructor = createUser(
                "Test",
                "Instructor",
                "instructor@example.com",
                Role.INSTRUCTOR
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

    private Course createDraftCourse() {
        Course course = new Course();
        course.setTitle("Draft Course");
        course.setDescription("This course is still a draft.");
        course.setInstructor(instructor);
        course.setStatus(CourseStatus.DRAFT);

        return courseRepository.save(course);
    }

    @Test
    void studentCanEnrollInPublishedCourse() throws Exception {
        Course course = createPublishedCourse();
        String token = tokenFor(student);

        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "courseId": "%s"
                                }
                                """.formatted(course.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId")
                        .value(course.getId().toString()))
                .andExpect(jsonPath("$.courseTitle")
                        .value("Java Fundamentals"))
                .andExpect(jsonPath("$.studentId")
                        .value(student.getId().toString()))
                .andExpect(jsonPath("$.status")
                        .value("ACTIVE"));
    }

    @Test
    void studentCannotEnrollInDraftCourse() throws Exception {
        Course course = createDraftCourse();
        String token = tokenFor(student);

        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "courseId": "%s"
                                }
                                """.formatted(course.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Only published courses can be enrolled in"));
    }

    @Test
    void studentCannotEnrollTwiceInSameCourse() throws Exception {
        Course course = createPublishedCourse();
        String token = tokenFor(student);

        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "courseId": "%s"
                                }
                                """.formatted(course.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "courseId": "%s"
                                }
                                """.formatted(course.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Student is already enrolled in this course"));
    }

    @Test
    void unauthenticatedUserCannotEnroll() throws Exception {
        Course course = createPublishedCourse();

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "courseId": "%s"
                                }
                                """.formatted(course.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void instructorCannotEnroll() throws Exception {
        Course course = createPublishedCourse();
        String token = tokenFor(instructor);

        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "courseId": "%s"
                                }
                                """.formatted(course.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotEnroll() throws Exception {
        Course course = createPublishedCourse();
        String token = tokenFor(admin);

        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "courseId": "%s"
                                }
                                """.formatted(course.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCanViewOwnEnrollments() throws Exception {
        Course course = createPublishedCourse();
        String token = tokenFor(student);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);

        enrollmentRepository.save(enrollment);

        mockMvc.perform(get("/api/enrollments/my-enrollments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].courseId")
                        .value(course.getId().toString()))
                .andExpect(jsonPath("$[0].studentId")
                        .value(student.getId().toString()))
                .andExpect(jsonPath("$[0].status")
                        .value("ACTIVE"));
    }

    @Test
    void instructorCanViewCourseEnrollments() throws Exception {
        Course course = createPublishedCourse();

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.save(enrollment);

        String token = tokenFor(instructor);

        mockMvc.perform(get("/api/enrollments/course/" + course.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].courseId")
                        .value(course.getId().toString()))
                .andExpect(jsonPath("$[0].studentId")
                        .value(student.getId().toString()))
                .andExpect(jsonPath("$[0].studentFirstName")
                        .value("Test"))
                .andExpect(jsonPath("$[0].studentLastName")
                        .value("Student"));
    }

    @Test
    void adminCanViewCourseEnrollments() throws Exception {
        Course course = createPublishedCourse();

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.save(enrollment);

        String token = tokenFor(admin);

        mockMvc.perform(get("/api/enrollments/course/" + course.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].courseId")
                        .value(course.getId().toString()))
                .andExpect(jsonPath("$[0].studentId")
                        .value(student.getId().toString()));
    }

    @Test
    void studentCannotViewCourseEnrollments() throws Exception {
        Course course = createPublishedCourse();
        String token = tokenFor(student);

        mockMvc.perform(get("/api/enrollments/course/" + course.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotViewCourseEnrollments() throws Exception {
        Course course = createPublishedCourse();

        mockMvc.perform(get("/api/enrollments/course/" + course.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void courseEnrollmentViewOnlyReturnsEnrollmentsForRequestedCourse()
            throws Exception {

        Course requestedCourse = createPublishedCourse();

        Course otherCourse = new Course();
        otherCourse.setTitle("Other Course");
        otherCourse.setDescription("Another published course.");
        otherCourse.setInstructor(instructor);
        otherCourse.setStatus(CourseStatus.PUBLISHED);
        otherCourse = courseRepository.save(otherCourse);

        Enrollment requestedEnrollment = new Enrollment();
        requestedEnrollment.setStudent(student);
        requestedEnrollment.setCourse(requestedCourse);
        enrollmentRepository.save(requestedEnrollment);

        Enrollment otherEnrollment = new Enrollment();
        otherEnrollment.setStudent(secondStudent);
        otherEnrollment.setCourse(otherCourse);
        enrollmentRepository.save(otherEnrollment);

        String token = tokenFor(instructor);

        mockMvc.perform(get("/api/enrollments/course/" + requestedCourse.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].courseId")
                        .value(requestedCourse.getId().toString()))
                .andExpect(jsonPath("$[0].studentId")
                        .value(student.getId().toString()));
    }

    @Test
    void instructorCannotViewAnotherInstructorsCourseEnrollments()
            throws Exception {

        Course course = createPublishedCourse();

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.save(enrollment);

        User secondInstructor = createUser(
                "Second",
                "Instructor",
                "second.instructor@example.com",
                Role.INSTRUCTOR
        );

        String token = tokenFor(secondInstructor);

        mockMvc.perform(get("/api/enrollments/course/" + course.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value(
                                "You do not have permission to view this course's enrollments"
                        ));
    }

    @Test
    void adminCanDropActiveEnrollment() throws Exception {
        Course course = createPublishedCourse();

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.save(enrollment);

        String token = tokenFor(admin);

        mockMvc.perform(patch("/api/enrollments/" + enrollment.getId() + "/drop")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(enrollment.getId().toString()))
                .andExpect(jsonPath("$.status")
                        .value("DROPPED"));
    }

    @Test
    void adminCannotDropAlreadyDroppedEnrollment() throws Exception {
        Course course = createPublishedCourse();

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollmentRepository.save(enrollment);

        String token = tokenFor(admin);

        mockMvc.perform(patch("/api/enrollments/" + enrollment.getId() + "/drop")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Only active enrollments can be dropped"));
    }

    @Test
    void studentCannotDropEnrollment() throws Exception {
        Course course = createPublishedCourse();

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.save(enrollment);

        String token = tokenFor(student);

        mockMvc.perform(patch("/api/enrollments/" + enrollment.getId() + "/drop")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void instructorCannotDropEnrollment() throws Exception {
        Course course = createPublishedCourse();

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.save(enrollment);

        String token = tokenFor(instructor);

        mockMvc.perform(patch("/api/enrollments/" + enrollment.getId() + "/drop")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotDropEnrollment() throws Exception {
        Course course = createPublishedCourse();

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.save(enrollment);

        mockMvc.perform(patch("/api/enrollments/" + enrollment.getId() + "/drop"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanCompleteActiveEnrollment() throws Exception {
        Course course = createPublishedCourse();

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.save(enrollment);

        String token = tokenFor(admin);

        mockMvc.perform(patch(
                        "/api/enrollments/" + enrollment.getId() + "/complete"
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(enrollment.getId().toString()))
                .andExpect(jsonPath("$.status")
                        .value("COMPLETED"));
    }

    @Test
    void adminCannotCompleteDroppedEnrollment() throws Exception {
        Course course = createPublishedCourse();

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollmentRepository.save(enrollment);

        String token = tokenFor(admin);

        mockMvc.perform(patch(
                        "/api/enrollments/" + enrollment.getId() + "/complete"
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Only active enrollments can be completed"));
    }

    @Test
    void adminCannotCompleteAlreadyCompletedEnrollment() throws Exception {
        Course course = createPublishedCourse();

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        enrollmentRepository.save(enrollment);

        String token = tokenFor(admin);

        mockMvc.perform(patch(
                        "/api/enrollments/" + enrollment.getId() + "/complete"
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Only active enrollments can be completed"));
    }

    @Test
    void studentCannotCompleteEnrollment() throws Exception {
        Course course = createPublishedCourse();

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.save(enrollment);

        String token = tokenFor(student);

        mockMvc.perform(patch(
                        "/api/enrollments/" + enrollment.getId() + "/complete"
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void instructorCannotCompleteEnrollment() throws Exception {
        Course course = createPublishedCourse();

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.save(enrollment);

        String token = tokenFor(instructor);

        mockMvc.perform(patch(
                        "/api/enrollments/" + enrollment.getId() + "/complete"
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotCompleteEnrollment() throws Exception {
        Course course = createPublishedCourse();

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.save(enrollment);

        mockMvc.perform(patch(
                        "/api/enrollments/" + enrollment.getId() + "/complete"
                ))
                .andExpect(status().isUnauthorized());
    }
}
