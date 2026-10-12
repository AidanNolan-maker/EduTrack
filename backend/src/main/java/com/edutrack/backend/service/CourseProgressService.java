package com.edutrack.backend.service;

import com.edutrack.backend.dto.CourseProgressResponse;
import com.edutrack.backend.entity.Course;
import com.edutrack.backend.entity.Enrollment;
import com.edutrack.backend.entity.EnrollmentStatus;
import com.edutrack.backend.entity.User;
import com.edutrack.backend.exception.BadRequestException;
import com.edutrack.backend.exception.ResourceNotFoundException;
import com.edutrack.backend.repository.CourseRepository;
import com.edutrack.backend.repository.EnrollmentRepository;
import com.edutrack.backend.repository.LessonCompletionRepository;
import com.edutrack.backend.repository.LessonRepository;
import com.edutrack.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseProgressService {
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonCompletionRepository lessonCompletionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CourseProgressResponse getCourseProgress(
            UUID courseId,
            UUID studentId
    ) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course Not Found"));

        User student = userRepository.findById(studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student Not Found"));

        Enrollment enrollment = enrollmentRepository
                .findByStudentId(studentId)
                .stream()
                .filter(existingEnrollment ->
                        existingEnrollment.getCourse().getId()
                                .equals(courseId))
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException(
                                "You are not enrolled in this course"
                        ));

        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            throw new BadRequestException(
                    "Your enrollment is not active"
            );
        }

        long totalLessons =
                lessonRepository.countByModuleCourseId(courseId);

        long completedLessons =
                lessonCompletionRepository
                        .countByStudentIdAndLessonModuleCourseId(
                                studentId,
                                courseId
                        );

        double progressPercentage = totalLessons == 0
                ? 0.0
                : (completedLessons * 100.0) / totalLessons;

        CourseProgressResponse response =
                new CourseProgressResponse();

        response.setCourseId(course.getId());
        response.setCourseTitle(course.getTitle());
        response.setTotalLessons((int) totalLessons);
        response.setCompletedLessons((int) completedLessons);
        response.setProgressPercentage(progressPercentage);

        return response;
    }
}
