package com.edutrack.backend.repository;

import com.edutrack.backend.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    List<Enrollment> findByStudentId(UUID studentId);

    List<Enrollment> findByCourseId(UUID courseId);
}
