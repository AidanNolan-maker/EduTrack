package com.edutrack.backend.service;

import com.edutrack.backend.dto.CreateLessonRequest;
import com.edutrack.backend.dto.LessonResponse;
import com.edutrack.backend.dto.UpdateLessonRequest;
import com.edutrack.backend.entity.Course;
import com.edutrack.backend.entity.CourseModule;
import com.edutrack.backend.entity.CourseStatus;
import com.edutrack.backend.entity.Lesson;
import com.edutrack.backend.mapper.LessonMapper;
import com.edutrack.backend.entity.Role;
import com.edutrack.backend.entity.User;
import com.edutrack.backend.exception.AccessDeniedException;
import com.edutrack.backend.exception.ResourceNotFoundException;
import com.edutrack.backend.repository.CourseModuleRepository;
import com.edutrack.backend.repository.LessonRepository;
import com.edutrack.backend.repository.LessonCompletionRepository;
import com.edutrack.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonService {
    private final LessonRepository lessonRepository;
    private final LessonCompletionRepository lessonCompletionRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final UserRepository userRepository;

    private final LessonMapper lessonMapper;

    public Lesson createLesson(
            CreateLessonRequest request,
            UUID userId
    ) {
        CourseModule module = courseModuleRepository
                .findById(request.getModuleId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Module not found"));

        Course course = module.getCourse();

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

        Lesson lesson = new Lesson();
        lesson.setModule(module);
        lesson.setTitle(request.getTitle());
        lesson.setContent(request.getContent());
        lesson.setDisplayOrder(request.getDisplayOrder());

        return lessonRepository.save(lesson);
    }

    public List<Lesson> getLessonsForModule(UUID moduleId) {
        CourseModule module = courseModuleRepository.findById(moduleId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Module not found"));

        if (module.getCourse().getStatus() != CourseStatus.PUBLISHED) {
            throw new AccessDeniedException(
                    "Course content is not publicly available"
            );
        }

        return lessonRepository
                .findByModuleIdOrderByDisplayOrderAsc(moduleId);
    }

    public LessonResponse updateLesson(
            UUID lessonId,
            UpdateLessonRequest request,
            UUID userId
    ) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson Not Found"));

        Course course = lesson.getModule().getCourse();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isOwner = course.getInstructor().getId().equals(userId);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException(
                    "You do not have permission to update this lesson"
            );
        }

        lesson.setTitle(request.getTitle());
        lesson.setContent(request.getContent());
        lesson.setDisplayOrder(request.getDisplayOrder());

        return lessonMapper.toResponse(lessonRepository.save(lesson));
    }

    public void deleteLesson(
            UUID lessonId,
            UUID userId
    ) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson Not Found"));

        Course course = lesson.getModule().getCourse();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isOwner = course.getInstructor().getId().equals(userId);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException(
                    "You do not have permission to delete this lesson"
            );
        }

        lessonCompletionRepository.deleteByLessonId(lessonId);
        lessonRepository.delete(lesson);
    }
}
