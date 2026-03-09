package com.capoo.identity.mapper;

import com.capoo.identity.dto.request.RoleRequest;
import com.capoo.identity.dto.response.RoleResponse;
import com.capoo.identity.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest request);

    RoleResponse toRoleResponse(Role role);
}
