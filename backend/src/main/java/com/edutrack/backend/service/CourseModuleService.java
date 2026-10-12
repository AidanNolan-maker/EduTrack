package com.edutrack.backend.service;

import com.edutrack.backend.dto.CreateModuleRequest;
import com.edutrack.backend.entity.Course;
import com.edutrack.backend.entity.CourseModule;
import com.edutrack.backend.entity.Role;
import com.edutrack.backend.entity.User;
import com.edutrack.backend.entity.CourseStatus;
import com.edutrack.backend.exception.AccessDeniedException;
import com.edutrack.backend.exception.ResourceNotFoundException;
import com.edutrack.backend.repository.CourseModuleRepository;
import com.edutrack.backend.repository.CourseRepository;
import com.edutrack.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseModuleService {
    private final CourseModuleRepository courseModuleRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CourseModule createModule(
            CreateModuleRequest request,
            UUID userId
    ) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Course not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isOwner = course.getInstructor().getId().equals(userId);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException(
                    "You do not have permission to manage this course"
            );
        }

        CourseModule courseModule = new CourseModule();
        courseModule.setCourse(course);
        courseModule.setTitle(request.getTitle());
        courseModule.setDisplayOrder(request.getDisplayOrder());

        return courseModuleRepository.save(courseModule);
    }

    public List<CourseModule> getModulesForCourse(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Course not found"));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new AccessDeniedException(
                    "Course content is not publicly available"
            );
        }

        return courseModuleRepository
                .findByCourseIdOrderByDisplayOrderAsc(courseId);
    }

    @Transactional
    public void deleteModule(UUID moduleId, UUID userId) {
        CourseModule module = courseModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"));

        Course course = module.getCourse();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isOwner = course.getInstructor().getId().equals(userId);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException(
                    "You do not have permission to delete this module"
            );
        }

        courseModuleRepository.delete(module);
    }
}
