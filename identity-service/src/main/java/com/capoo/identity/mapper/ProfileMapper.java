package com.capoo.identity.mapper;

import com.capoo.identity.dto.request.RoleRequest;
import com.capoo.identity.dto.request.UserCreationRequest;
import com.capoo.identity.dto.request.UserProfileCreationRequest;
import com.capoo.identity.dto.response.RoleResponse;
import com.capoo.identity.entity.Role;
import com.capoo.identity.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    @Mapping(source = "id", target = "userId")
    UserProfileCreationRequest toUserProfileCreationRequest(User user);

    UserProfileCreationRequest toUserProfileCreationRequest(UserCreationRequest userCreationRequest);
}

