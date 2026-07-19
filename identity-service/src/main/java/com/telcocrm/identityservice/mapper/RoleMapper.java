package com.telcocrm.identityservice.mapper;

import com.telcocrm.identityservice.dto.response.RoleResponse;
import com.telcocrm.identityservice.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "permissions", source = "permissions")
    RoleResponse toResponse(Role role, List<String> permissions);
}
