package com.capoo.identity.mapper;

import com.capoo.identity.dto.request.PermissionRequest;
import com.capoo.identity.dto.response.PermissionResponse;
import com.capoo.identity.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);
}
