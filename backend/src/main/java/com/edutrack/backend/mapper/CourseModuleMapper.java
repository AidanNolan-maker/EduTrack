package com.edutrack.backend.mapper;

import com.edutrack.backend.dto.ModuleResponse;
import com.edutrack.backend.entity.CourseModule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourseModuleMapper {
    @Mapping(target = "courseId", source = "course.id")
    ModuleResponse toResponse(CourseModule courseModule);
}
