package com.edutrack.backend.controller;

import com.edutrack.backend.entity.Course;
import com.edutrack.backend.entity.CourseModule;
import com.edutrack.backend.entity.CourseStatus;
import com.edutrack.backend.entity.Enrollment;
import com.edutrack.backend.entity.EnrollmentStatus;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseProgressControllerIntegrationTest {

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

    private Lesson createLesson(
            CourseModule module,
            String title,
            int displayOrder
    ) {
        Lesson lesson = new Lesson();
        lesson.setModule(module);
        lesson.setTitle(title);
        lesson.setContent("Lesson content.");
        lesson.setDisplayOrder(displayOrder);

        return lessonRepository.save(lesson);
    }

    private Enrollment createEnrollment(
            User student,
            Course course,
            EnrollmentStatus status
    ) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus(status);

        return enrollmentRepository.save(enrollment);
    }

    private LessonCompletion createCompletion(
            User student,
            Lesson lesson
    ) {
        LessonCompletion completion = new LessonCompletion();
        completion.setStudent(student);
        completion.setLesson(lesson);

        return lessonCompletionRepository.save(completion);
    }

    @Test
    void studentWithNoCompletedLessonsHasZeroProgress()
            throws Exception {

        Course course = createPublishedCourse();
        CourseModule module = createModule(course);

        createLesson(module, "Variables", 1);
        createLesson(module, "Methods", 2);

        createEnrollment(
                student,
                course,
                EnrollmentStatus.ACTIVE
        );

        String token = tokenFor(student);

        mockMvc.perform(get(
                        "/api/courses/" + course.getId() + "/progress"
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId")
                        .value(course.getId().toString()))
                .andExpect(jsonPath("$.courseTitle")
                        .value("Java Fundamentals"))
                .andExpect(jsonPath("$.totalLessons")
                        .value(2))
                .andExpect(jsonPath("$.completedLessons")
                        .value(0))
                .andExpect(jsonPath("$.progressPercentage")
                        .value(0.0));
    }

    @Test
    void studentWithHalfCompletedLessonsHasFiftyPercentProgress()
            throws Exception {

        Course course = createPublishedCourse();
        CourseModule module = createModule(course);

        Lesson firstLesson =
                createLesson(module, "Variables", 1);

        createLesson(module, "Methods", 2);

        createEnrollment(
                student,
                course,
                EnrollmentStatus.ACTIVE
        );

        createCompletion(student, firstLesson);

        String token = tokenFor(student);

        mockMvc.perform(get(
                        "/api/courses/" + course.getId() + "/progress"
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLessons")
                        .value(2))
                .andExpect(jsonPath("$.completedLessons")
                        .value(1))
                .andExpect(jsonPath("$.progressPercentage")
                        .value(50.0));
    }

    @Test
    void studentWithAllCompletedLessonsHasOneHundredPercentProgress()
            throws Exception {

        Course course = createPublishedCourse();
        CourseModule module = createModule(course);

        Lesson firstLesson =
                createLesson(module, "Variables", 1);

        Lesson secondLesson =
                createLesson(module, "Methods", 2);

        createEnrollment(
                student,
                course,
                EnrollmentStatus.ACTIVE
        );

        createCompletion(student, firstLesson);
        createCompletion(student, secondLesson);

        String token = tokenFor(student);

        mockMvc.perform(get(
                        "/api/courses/" + course.getId() + "/progress"
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLessons")
                        .value(2))
                .andExpect(jsonPath("$.completedLessons")
                        .value(2))
                .andExpect(jsonPath("$.progressPercentage")
                        .value(100.0));
    }

    @Test
    void anotherStudentsCompletionsDoNotAffectProgress()
            throws Exception {

        Course course = createPublishedCourse();
        CourseModule module = createModule(course);

        Lesson firstLesson =
                createLesson(module, "Variables", 1);

        createLesson(module, "Methods", 2);

        createEnrollment(
                student,
                course,
                EnrollmentStatus.ACTIVE
        );

        createEnrollment(
                secondStudent,
                course,
                EnrollmentStatus.ACTIVE
        );

        createCompletion(secondStudent, firstLesson);

        String token = tokenFor(student);

        mockMvc.perform(get(
                        "/api/courses/" + course.getId() + "/progress"
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLessons")
                        .value(2))
                .andExpect(jsonPath("$.completedLessons")
                        .value(0))
                .andExpect(jsonPath("$.progressPercentage")
                        .value(0.0));
    }

    @Test
    void unenrolledStudentCannotViewCourseProgress()
            throws Exception {

        Course course = createPublishedCourse();

        String token = tokenFor(student);

        mockMvc.perform(get(
                        "/api/courses/" + course.getId() + "/progress"
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("You are not enrolled in this course"));
    }

    @Test
    void droppedStudentCannotViewCourseProgress()
            throws Exception {

        Course course = createPublishedCourse();

        createEnrollment(
                student,
                course,
                EnrollmentStatus.DROPPED
        );

        String token = tokenFor(student);

        mockMvc.perform(get(
                        "/api/courses/" + course.getId() + "/progress"
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Your enrollment is not active"));
    }

    @Test
    void completedStudentCanViewFinalCourseProgress()
            throws Exception {

        Course course = createPublishedCourse();
        CourseModule module = createModule(course);

        Lesson lesson =
                createLesson(module, "Variables", 1);

        createEnrollment(
                student,
                course,
                EnrollmentStatus.COMPLETED
        );

        createCompletion(student, lesson);

        String token = tokenFor(student);

        mockMvc.perform(get(
                        "/api/courses/" + course.getId() + "/progress"
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLessons")
                        .value(1))
                .andExpect(jsonPath("$.completedLessons")
                        .value(1))
                .andExpect(jsonPath("$.progressPercentage")
                        .value(100.0));
    }

    @Test
    void courseWithNoLessonsHasZeroProgress()
            throws Exception {

        Course course = createPublishedCourse();

        createEnrollment(
                student,
                course,
                EnrollmentStatus.ACTIVE
        );

        String token = tokenFor(student);

        mockMvc.perform(get(
                        "/api/courses/" + course.getId() + "/progress"
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLessons")
                        .value(0))
                .andExpect(jsonPath("$.completedLessons")
                        .value(0))
                .andExpect(jsonPath("$.progressPercentage")
                        .value(0.0));
    }

    @Test
    void instructorCannotViewCourseProgress()
            throws Exception {

        Course course = createPublishedCourse();

        String token = tokenFor(instructor);

        mockMvc.perform(get(
                        "/api/courses/" + course.getId() + "/progress"
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotViewCourseProgress()
            throws Exception {

        Course course = createPublishedCourse();

        String token = tokenFor(admin);

        mockMvc.perform(get(
                        "/api/courses/" + course.getId() + "/progress"
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotViewCourseProgress()
            throws Exception {

        Course course = createPublishedCourse();

        mockMvc.perform(get(
                        "/api/courses/" + course.getId() + "/progress"
                ))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonexistentCourseReturnsNotFound()
            throws Exception {

        String token = tokenFor(student);

        mockMvc.perform(get(
                        "/api/courses/"
                                + "00000000-0000-0000-0000-000000000000"
                                + "/progress"
                )
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Course Not Found"));
    }
}