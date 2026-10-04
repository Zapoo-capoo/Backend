package com.capoo.profile.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.capoo.event.dto.UserProfileCreationRequest;
import com.capoo.profile.dto.request.SearchUserRequest;
import com.capoo.profile.dto.request.UpdateProfileRequest;
import com.capoo.profile.dto.response.UserProfileReponse;

public interface UserProfileService {

    void createProfile(UserProfileCreationRequest request);

    UserProfileReponse createUserProfile(UserProfileCreationRequest request);

    UserProfileReponse getProfileByUserId(String userId);

    UserProfileReponse getUserProfile(String profileId);

    List<UserProfileReponse> getUserProfiles();

    List<UserProfileReponse> getAllProfiles();

    UserProfileReponse getMyProfile();

    UserProfileReponse updateMyProfile(UpdateProfileRequest request);

    UserProfileReponse updateAvatar(MultipartFile file);

    List<UserProfileReponse> search(SearchUserRequest request);
}
