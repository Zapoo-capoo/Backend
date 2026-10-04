package com.capoo.identity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.capoo.identity.dto.request.RoleRequest;
import com.capoo.identity.dto.response.RoleResponse;
import com.capoo.identity.entity.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest request);

    RoleResponse toRoleResponse(Role role);
}
