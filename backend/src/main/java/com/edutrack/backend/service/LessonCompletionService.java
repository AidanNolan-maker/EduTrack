package com.edutrack.backend.service;

import com.edutrack.backend.entity.Course;
import com.edutrack.backend.entity.Enrollment;
import com.edutrack.backend.entity.EnrollmentStatus;
import com.edutrack.backend.entity.Lesson;
import com.edutrack.backend.entity.LessonCompletion;
import com.edutrack.backend.entity.User;
import com.edutrack.backend.exception.BadRequestException;
import com.edutrack.backend.exception.ResourceNotFoundException;
import com.edutrack.backend.repository.EnrollmentRepository;
import com.edutrack.backend.repository.LessonCompletionRepository;
import com.edutrack.backend.repository.LessonRepository;
import com.edutrack.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonCompletionService {
    private final LessonCompletionRepository lessonCompletionRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public LessonCompletion completeLesson(
            UUID lessonId,
            UUID studentId
    ) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Lesson not found"));

        User student = userRepository.findById(studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student not found"));

        Course course = lesson.getModule().getCourse();

        Enrollment enrollment = enrollmentRepository
                .findByStudentId(studentId)
                .stream()
                .filter(existingEnrollment ->
                        existingEnrollment.getCourse().getId()
                                .equals(course.getId()))
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException(
                                "You are not enrolled in this course"
                        ));

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BadRequestException(
                    "Your enrollment is not active"
            );
        }

        if (lessonCompletionRepository
                .existsByStudentIdAndLessonId(studentId, lessonId)) {
            throw new BadRequestException(
                    "Lesson has already been completed"
            );
        }

        LessonCompletion completion = new LessonCompletion();
        completion.setStudent(student);
        completion.setLesson(lesson);

        return lessonCompletionRepository.save(completion);
    }

    @Transactional(readOnly = true)
    public List<LessonCompletion> getStudentCompletions(
            UUID studentId
    ) {
        if (!userRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found");
        }

        return lessonCompletionRepository.findByStudentId(studentId);
    }
}
