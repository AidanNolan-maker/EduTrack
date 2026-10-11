package com.edutrack.backend.mapper;

import com.edutrack.backend.dto.EnrollmentResponse;
import com.edutrack.backend.entity.Enrollment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseTitle", source = "course.title")
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentFirstName", source = "student.firstName")
    @Mapping(target = "studentLastName", source = "student.lastName")
    EnrollmentResponse toResponse(Enrollment enrollment);
}
