package com.capoo.profile.mapper;

import com.capoo.profile.dto.request.UserProfileCreationRequest;
import com.capoo.profile.dto.response.UserProfileReponse;
import com.capoo.profile.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.context.annotation.Profile;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "userId", target = "userId")

    UserProfile toUserProfile(UserProfileCreationRequest request);
    UserProfileReponse toUserProfileResponse(UserProfile userProfile);

    // @Mapping(target = "roles", ignore = true)
    //   void updateUser(@MappingTarget UserProfile user, User request);
}
