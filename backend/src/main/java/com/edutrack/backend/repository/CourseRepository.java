package com.edutrack.backend.repository;

import com.edutrack.backend.entity.Course;
import com.edutrack.backend.entity.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findByInstructorId(UUID instructorId);

    List<Course> findByStatus(CourseStatus status);
}
