package com.telcocrm.identityservice.service;

import com.telcocrm.identityservice.dto.request.CreatePermissionRequest;
import com.telcocrm.identityservice.dto.response.PermissionResponse;

import java.util.List;

public interface PermissionService {

    PermissionResponse createPermission(CreatePermissionRequest request);

    List<PermissionResponse> listPermissions();
}
