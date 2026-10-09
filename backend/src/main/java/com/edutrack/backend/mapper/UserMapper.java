package com.edutrack.backend.mapper;

import com.edutrack.backend.dto.UserResponse;
import com.edutrack.backend.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
