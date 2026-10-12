package com.edutrack.backend.mapper;

import com.edutrack.backend.dto.LessonResponse;
import com.edutrack.backend.entity.Lesson;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LessonMapper {
    @Mapping(target = "moduleId", source = "module.id")
    @Mapping(target = "courseId", source = "module.course.id")
    LessonResponse toResponse(Lesson lesson);
}
