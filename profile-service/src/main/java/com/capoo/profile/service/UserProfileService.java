package com.capoo.profile.service;


import com.capoo.profile.dto.request.SearchUserRequest;
import com.capoo.profile.dto.request.UpdateProfileRequest;
import com.capoo.profile.dto.request.UserProfileCreationRequest;
import com.capoo.profile.dto.request.UpdateParticipantRequest;
import com.capoo.profile.dto.response.UserProfileReponse;
import com.capoo.profile.entity.UserProfile;
import com.capoo.profile.exception.AppException;
import com.capoo.profile.exception.ErrorCode;
import com.capoo.profile.mapper.UserProfileMapper;
import com.capoo.profile.repository.UserProfileRepository;
import com.capoo.profile.repository.httpClient.ChatClient;
import com.capoo.profile.repository.httpClient.FileClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileService {
    final UserProfileMapper userProfileMapper;
    final UserProfileRepository userProfileRepository;
    final FileClient fileClient;
    final ChatClient chatClient;
    public UserProfileReponse createUserProfile(UserProfileCreationRequest request ) {
        UserProfile userProfile = userProfileMapper.toUserProfile(request);
        userProfile = userProfileRepository.save(userProfile);
        log.info("UserProfile{}",userProfile.toString());
        return userProfileMapper.toUserProfileResponse(userProfile);
    }
    public UserProfileReponse getProfileByUserId(String userId) {
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));
        return userProfileMapper.toUserProfileResponse(userProfile);
    }
    public UserProfileReponse getUserProfile(String profileId) {
        UserProfile userProfile = userProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));
        return userProfileMapper.toUserProfileResponse(userProfile);
    }
    public List<UserProfileReponse> getUserProfiles() {
        log.info("In method get Users");
        return userProfileRepository.findAll().stream().map(userProfileMapper::toUserProfileResponse).toList();
    }
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserProfileReponse> getAllProfiles() {
        var profiles = userProfileRepository.findAll();

        return profiles.stream().map(userProfileMapper::toUserProfileResponse).toList();
    }

    public UserProfileReponse getMyProfile() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        var profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userProfileMapper.toUserProfileResponse(profile);
    }
    public UserProfileReponse updateMyProfile(UpdateProfileRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        var profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        userProfileMapper.update(profile, request);

        UserProfile updated = userProfileRepository.save(profile);

        // notify chat service to update participant info
        UpdateParticipantRequest updateParticipantRequest = UpdateParticipantRequest.builder()
                .userId(updated.getUserId())
                .username(updated.getUsername())
                .firstName(updated.getFirstName())
                .lastName(updated.getLastName())
                .avatar(updated.getAvatar())
                .build();
        try {
            chatClient.updateParticipant(updateParticipantRequest);
        } catch (Exception ex) {
            log.warn("Failed to notify chat service about profile update: {}", ex.getMessage());
        }

        return userProfileMapper.toUserProfileResponse(updated);
    }

    public UserProfileReponse updateAvatar(MultipartFile file) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        var profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        var response = fileClient.uploadMedia(file);

        profile.setAvatar(response.getResult().getUrl());

        UserProfile updated = userProfileRepository.save(profile);

        // notify chat service to update participant avatar
        UpdateParticipantRequest updateParticipantRequest = UpdateParticipantRequest.builder()
                .userId(updated.getUserId())
                .username(updated.getUsername())
                .firstName(updated.getFirstName())
                .lastName(updated.getLastName())
                .avatar(updated.getAvatar())
                .build();
        try {
            chatClient.updateParticipant(updateParticipantRequest);
        } catch (Exception ex) {
            log.warn("Failed to notify chat service about avatar update: {}", ex.getMessage());
        }

        return userProfileMapper.toUserProfileResponse(updated);
    }

    public List<UserProfileReponse> search(SearchUserRequest request) {
        // Get current authenticated user's id
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        // Find the profile node for the current user to obtain the internal profile id
        UserProfile myProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Retrieve friends of the current profile (returns list of UserProfile nodes)
        List<UserProfile> friends = userProfileRepository.getFriend(myProfile.getId());

        // If keyword is null, treat it as empty string to return all friends
        String keyword = request.getKeyword() == null ? "" : request.getKeyword();

        // Filter friends by username like the original search behavior and map to response
        return friends.stream()
                .filter(fp -> fp.getUsername() != null && fp.getUsername().contains(keyword))
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }
}
