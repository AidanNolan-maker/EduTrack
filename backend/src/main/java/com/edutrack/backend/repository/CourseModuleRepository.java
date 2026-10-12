package com.edutrack.backend.repository;

import com.edutrack.backend.entity.CourseModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseModuleRepository extends JpaRepository<CourseModule, UUID> {
    List<CourseModule> findByCourseIdOrderByDisplayOrderAsc(UUID courseId);
}
