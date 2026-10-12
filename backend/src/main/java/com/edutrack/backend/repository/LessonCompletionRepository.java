package com.edutrack.backend.repository;

import com.edutrack.backend.entity.LessonCompletion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LessonCompletionRepository
        extends JpaRepository<LessonCompletion, UUID> {
    boolean existsByStudentIdAndLessonId(
            UUID studentId,
            UUID lessonId
    );

    List<LessonCompletion> findByStudentId(UUID studentId);

    List<LessonCompletion> findByStudentIdAndLessonId(
            UUID studentId,
            UUID lessonId
    );

    long countByStudentIdAndLessonModuleCourseId(
            UUID studentId,
            UUID courseId
    );
}
