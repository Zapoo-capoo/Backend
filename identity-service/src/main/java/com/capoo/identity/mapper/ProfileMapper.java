package com.capoo.identity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.capoo.event.dto.UserProfileCreationRequest;
import com.capoo.identity.dto.request.UserCreationRequest;
import com.capoo.identity.entity.User;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    @Mapping(source = "id", target = "userId")
    UserProfileCreationRequest toUserProfileCreationRequest(User user);

    UserProfileCreationRequest toUserProfileCreationRequest(UserCreationRequest userCreationRequest);
}
