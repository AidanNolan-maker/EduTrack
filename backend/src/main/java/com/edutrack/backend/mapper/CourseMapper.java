package com.edutrack.backend.mapper;

import com.edutrack.backend.dto.CourseResponse;
import com.edutrack.backend.entity.Course;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourseMapper {
    @Mapping(target = "instructorId", source = "instructor.id")
    @Mapping(target = "instructorFirstName", source = "instructor.firstName")
    @Mapping(target = "instructorLastName", source = "instructor.lastName")
    @Mapping(target = "courseStatus", source = "status")
    CourseResponse toResponse(Course course);
}
