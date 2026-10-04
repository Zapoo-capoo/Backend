package com.capoo.identity.mapper;

import org.mapstruct.Mapper;

import com.capoo.identity.dto.request.PermissionRequest;
import com.capoo.identity.dto.response.PermissionResponse;
import com.capoo.identity.entity.Permission;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);
}
