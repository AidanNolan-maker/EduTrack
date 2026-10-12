package com.edutrack.backend.repository;

import com.edutrack.backend.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findByModuleIdOrderByDisplayOrderAsc(UUID moduleId);

    List<Lesson> findByModuleCourseId(UUID courseId);

    long countByModuleCourseId(UUID courseId);
}
