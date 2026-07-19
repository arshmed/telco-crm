package com.telcocrm.identityservice.mapper;

import com.telcocrm.identityservice.dto.response.UserResponse;
import com.telcocrm.identityservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", source = "roles")
    UserResponse toResponse(User user, List<String> roles);
}
