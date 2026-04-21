package com.capoo.profile.service;

import com.capoo.profile.dto.response.FriendRequestReponse;
import com.capoo.profile.dto.response.UserProfileReponse;
import com.capoo.profile.entity.UserProfile;
import com.capoo.profile.mapper.UserProfileMapper;
import com.capoo.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class FriendService {

    private final UserProfileRepository repository;
    private final UserProfileMapper userProfileMapper;

    public FriendRequestReponse sendRequest(String username) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        String fromId=repository.findByUserId(userId).orElseThrow(() -> new RuntimeException("User profile not found")).getId();
        String toProfileId=repository.findByUsername(username).orElseThrow(() -> new RuntimeException("User profile not found")).getId();
        String status=repository.getFriendStatus(fromId, toProfileId);
        if (status.equals("NONE")) {
            repository.sendFriendRequest(fromId, toProfileId);
            status = "SENT";
        }
        if (status.equals("RECEIVED")) {
            repository.acceptFriend(fromId, toProfileId);
            status = "FRIEND";
        }
        return FriendRequestReponse.builder()
                .fromId(fromId)
                .toId(toProfileId)
                .status(status)
                .build();
    }
    public FriendRequestReponse reject(String toProfileId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        String fromId=repository.findByUserId(userId).orElseThrow(() -> new RuntimeException("User profile not found")).getId();
        String status=repository.getFriendStatus(fromId, toProfileId);
        if (status.equals("RECEIVED")) {
            repository.rejectFriend(fromId, toProfileId);
            status = "NONE";
        }
        return FriendRequestReponse.builder()
                .fromId(fromId)
                .toId(toProfileId)
                .status(status)
                .build();
    }
    public FriendRequestReponse unfriend(String toProfileId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        String fromId=repository.findByUserId(userId).orElseThrow(() -> new RuntimeException("User profile not found")).getId();
        String status=repository.getFriendStatus(fromId, toProfileId);
        if (status.equals("FRIEND")) {
            repository.unfriend(fromId, toProfileId);
            status = "NONE";
        }
        return FriendRequestReponse.builder()
                .fromId(fromId)
                .toId(toProfileId)
                .status(status)
                .build();
    }


    public List<UserProfileReponse> getSentRequests() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        String id=repository.findByUserId(userId).orElseThrow(() -> new RuntimeException("User profile not found")).getId();
        List<UserProfile> userProfiles= repository.getSentRequests(id);
        return userProfiles.stream()
                .filter(userProfile -> !userId.equals(userProfile.getUserId()))
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }
    public List<UserProfileReponse> getReceivedRequests() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        String id=repository.findByUserId(userId).orElseThrow(() -> new RuntimeException("User profile not found")).getId();
        List<UserProfile> userProfiles= repository.getReceivedRequests(id);
        return userProfiles.stream()
                .filter(userProfile -> !userId.equals(userProfile.getUserId()))
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }
    public List<UserProfileReponse> getAllFriendRequests() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        String id=repository.findByUserId(userId).orElseThrow(() -> new RuntimeException("User profile not found")).getId();
        List<UserProfile> userProfiles= repository.getFriend(id);
        return userProfiles.stream()
                .filter(userProfile -> !userId.equals(userProfile.getUserId()))
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }
    public List<UserProfileReponse> getAllFriendRequestsById(String profileId) {
        String userId = profileId;
        String id=repository.findByUserId(userId).orElseThrow(() -> new RuntimeException("User profile not found")).getId();
        List<UserProfile> userProfiles= repository.getFriend(id);
        return userProfiles.stream()
                .filter(userProfile -> !userId.equals(userProfile.getUserId()))
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }
}