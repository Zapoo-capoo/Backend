package com.capoo.profile.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.capoo.event.dto.NotificationEvent;
import com.capoo.profile.dto.response.FriendRequestReponse;
import com.capoo.profile.dto.response.UserProfileReponse;
import com.capoo.profile.entity.UserProfile;
import com.capoo.profile.mapper.UserProfileMapper;
import com.capoo.profile.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final UserProfileRepository repository;
    private final UserProfileMapper userProfileMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.notification-events:notification.events}")
    private String notificationTopic;

    @Override
    public FriendRequestReponse sendRequest(String username) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        UserProfile from = repository
                .findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));
        UserProfile to =
                repository.findByUsername(username).orElseThrow(() -> new RuntimeException("User profile not found"));
        String fromId = from.getId();
        String toProfileId = to.getId();
        String status = repository.getFriendStatus(fromId, toProfileId);
        if (status.equals("NONE")) {
            repository.sendFriendRequest(fromId, toProfileId);
            status = "SENT";
            notifyFriendEvent("FRIEND_REQUEST", from, to);
        }
        if (status.equals("RECEIVED")) {
            repository.acceptFriend(fromId, toProfileId);
            status = "FRIEND";
            // "from" accepted the request "to" had sent, so "to" is the one to tell
            notifyFriendEvent("FRIEND_ACCEPTED", from, to);
        }
        return FriendRequestReponse.builder()
                .fromId(fromId)
                .toId(toProfileId)
                .status(status)
                .build();
    }

    /** Tells notification-service that {@code actor} did something to {@code recipient}. Never breaks the request. */
    private void notifyFriendEvent(String type, UserProfile actor, UserProfile recipient) {
        if (actor.getUserId() == null || recipient.getUserId() == null) return;
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .type(type)
                    .actorId(actor.getUserId())
                    .actorName(displayName(actor))
                    .actorAvatar(actor.getAvatar())
                    .recipientIds(List.of(recipient.getUserId()))
                    .targetType("USER")
                    .targetId(actor.getUserId())
                    // Text shown after the actor's name, like "Nguyen Van A đã gửi lời mời kết bạn"
                    .data(Map.of("body", "FRIEND_REQUEST".equals(type) ? "đã gửi lời mời kết bạn" : "đã đồng ý kết bạn"))
                    .build();
            kafkaTemplate
                    .send(notificationTopic, recipient.getUserId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) log.error("Failed to publish {} notification event", type, ex);
                    });
        } catch (Exception e) {
            log.error("Failed to publish {} notification event", type, e);
        }
    }

    private String displayName(UserProfile profile) {
        String fullName = ((profile.getFirstName() == null ? "" : profile.getFirstName()) + " "
                        + (profile.getLastName() == null ? "" : profile.getLastName()))
                .trim();
        return fullName.isEmpty() ? profile.getUsername() : fullName;
    }

    @Override
    public FriendRequestReponse reject(String toProfileId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        String fromId = repository
                .findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"))
                .getId();
        String status = repository.getFriendStatus(fromId, toProfileId);
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

    @Override
    public FriendRequestReponse unfriend(String toProfileId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        String fromId = repository
                .findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"))
                .getId();
        String status = repository.getFriendStatus(fromId, toProfileId);
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

    @Override
    public List<UserProfileReponse> getSentRequests() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        String id = repository
                .findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"))
                .getId();
        List<UserProfile> userProfiles = repository.getSentRequests(id);
        return userProfiles.stream()
                .filter(userProfile -> !userId.equals(userProfile.getUserId()))
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }

    @Override
    public List<UserProfileReponse> getReceivedRequests() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        String id = repository
                .findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"))
                .getId();
        List<UserProfile> userProfiles = repository.getReceivedRequests(id);
        return userProfiles.stream()
                .filter(userProfile -> !userId.equals(userProfile.getUserId()))
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }

    @Override
    public List<UserProfileReponse> getAllFriendRequests() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        String id = repository
                .findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"))
                .getId();
        List<UserProfile> userProfiles = repository.getFriend(id);
        return userProfiles.stream()
                .filter(userProfile -> !userId.equals(userProfile.getUserId()))
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }

    @Override
    public List<UserProfileReponse> getAllFriendRequestsById(String profileId) {
        String userId = profileId;
        String id = repository
                .findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"))
                .getId();
        List<UserProfile> userProfiles = repository.getFriend(id);
        return userProfiles.stream()
                .filter(userProfile -> !userId.equals(userProfile.getUserId()))
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }
}
