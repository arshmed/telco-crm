package com.telcocrm.identityservice.service;

import com.telcocrm.identityservice.dto.request.CreateRoleRequest;
import com.telcocrm.identityservice.dto.response.RoleResponse;

import java.util.List;

public interface RoleService {

    RoleResponse createRole(CreateRoleRequest request);

    RoleResponse getRoleByName(String name);

    List<RoleResponse> listRoles();

    RoleResponse assignPermission(String roleName, String permissionName);
}
