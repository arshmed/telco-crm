package com.telcocrm.identityservice.mapper;

import com.telcocrm.identityservice.dto.response.PermissionResponse;
import com.telcocrm.identityservice.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionResponse toResponse(Permission permission);
}
