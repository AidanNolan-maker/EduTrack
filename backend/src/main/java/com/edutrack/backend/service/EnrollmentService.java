package com.edutrack.backend.service;

import com.edutrack.backend.dto.EnrollmentRequest;
import com.edutrack.backend.entity.Course;
import com.edutrack.backend.entity.CourseStatus;
import com.edutrack.backend.entity.Enrollment;
import com.edutrack.backend.entity.EnrollmentStatus;
import com.edutrack.backend.entity.Role;
import com.edutrack.backend.entity.User;
import com.edutrack.backend.exception.AccessDeniedException;
import com.edutrack.backend.exception.BadRequestException;
import com.edutrack.backend.repository.CourseRepository;
import com.edutrack.backend.repository.EnrollmentRepository;
import com.edutrack.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public Enrollment enrollStudent(
            EnrollmentRequest request,
            UUID studentId
    ) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Student not found"));

        if (student.getRole() != Role.STUDENT) {
            throw new BadRequestException(
                    "Only students can enroll in courses"
            );
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Course not found"));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new BadRequestException(
                    "Only published courses can be enrolled in"
            );
        }

        if (enrollmentRepository.existsByStudentIdAndCourseId(
                studentId,
                course.getId()
        )) {
            throw new BadRequestException(
                    "Student is already enrolled in this course"
            );
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        return enrollmentRepository.save(enrollment);
    }

    public List<Enrollment> getStudentEnrollments(UUID studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    public List<Enrollment> getCourseEnrollments(
            UUID courseId,
            UUID userId
    ) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Course not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isOwner = course.getInstructor().getId().equals(userId);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException(
                    "You do not have permission to view this course's enrollments"
            );
        }

        return enrollmentRepository.findByCourseId(courseId);
    }

    public Enrollment dropEnrollment(UUID enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Enrollment not found"));

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BadRequestException(
                    "Only active enrollments can be dropped"
            );
        }

        enrollment.setStatus(EnrollmentStatus.DROPPED);

        return enrollmentRepository.save(enrollment);
    }
}
