package com.edutrack.backend.service;

import com.edutrack.backend.dto.CreateCourseRequest;
import com.edutrack.backend.entity.Course;
import com.edutrack.backend.entity.CourseStatus;
import com.edutrack.backend.entity.User;
import com.edutrack.backend.entity.Role;
import com.edutrack.backend.exception.BadRequestException;
import com.edutrack.backend.repository.CourseRepository;
import com.edutrack.backend.repository.UserRepository;
import com.edutrack.backend.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public Course createCourse(
            CreateCourseRequest request,
            UUID instructorId
    ) {
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Instructor not found"));

        if (instructor.getRole() != Role.INSTRUCTOR &&
                instructor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException(
                    "Only instructors and admins can create courses"
            );
        }

        Course course = new Course();
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setInstructor(instructor);
        course.setStatus(CourseStatus.DRAFT);

        return courseRepository.save(course);
    }

    public List<Course> getPublishedCourses() {
        return courseRepository.findByStatus(CourseStatus.PUBLISHED);
    }

    public List<Course> getCoursesByInstructor(UUID instructorId) {
        return courseRepository.findByInstructorId(instructorId);
    }

    public Course publishCourse(UUID courseId, UUID userId) {
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
                    "You do not have permission to publish this course"
            );
        }

        if (course.getStatus() != CourseStatus.DRAFT) {
            throw new BadRequestException(
                    "Only draft courses can be published"
            );
        }

        course.setStatus(CourseStatus.PUBLISHED);

        return courseRepository.save(course);
    }
}
