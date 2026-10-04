package com.capoo.chat.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.capoo.chat.constant.ConversationType;
import com.capoo.chat.dto.request.AddParticipantsRequest;
import com.capoo.chat.dto.request.ConversationRequest;
import com.capoo.chat.dto.request.GroupConversationRequest;
import com.capoo.chat.dto.request.UpdateParticipantRequest;
import com.capoo.chat.dto.response.ConversationResponse;
import com.capoo.chat.dto.response.UserProfileResponse;
import com.capoo.chat.entity.Conversation;
import com.capoo.chat.entity.ParticipantInfo;
import com.capoo.chat.exception.AppException;
import com.capoo.chat.exception.ErrorCode;
import com.capoo.chat.mapper.ConversationMapper;
import com.capoo.chat.repository.ConversationRepository;
import com.capoo.chat.repository.httpclient.ProfileClient;
import com.capoo.chat.service.cache.ChatCacheKeys;
import com.capoo.chat.service.cache.ICacheService;

import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationServiceImpl implements ConversationService {
    ConversationRepository conversationRepository;
    ProfileClient profileClient;
    ICacheService cacheService;

    ConversationMapper conversationMapper;

    @Override
    public List<ConversationResponse> myConversations() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Conversation> conversations = conversationRepository.findAllByParticipantIdsContains(userId);

        return conversations.stream().map(this::toConversationResponse).toList();
    }

    @Override
    public ConversationResponse create(ConversationRequest request) {
        // Fetch user info
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        var userProfileResponse = profileClient.getProfile(userId);
        var participantInfoResponses =
                profileClient.getProfile(request.getParticipantIds().getFirst());
        if (Objects.isNull(participantInfoResponses) || Objects.isNull(participantInfoResponses.getResult())) {
            throw new RuntimeException("Participant not found");
        }
        var userInfo = userProfileResponse.getResult();
        var participantInfo = participantInfoResponses.getResult();

        List<String> userIds = new ArrayList<>();
        userIds.add(userId);
        userIds.add(request.getParticipantIds().getFirst());

        var sortedId = userIds.stream().sorted().toList();
        String userIdsHash = generateParticipantHash(sortedId);

        var conversationOptional = conversationRepository.findByParticipantsHash(userIdsHash);
        if (conversationOptional.isPresent()) {
            return toConversationResponse(conversationOptional.get());
        }

        List<ParticipantInfo> participantInfoList =
                List.of(toParticipantInfo(userInfo), toParticipantInfo(participantInfo));
        // Build conversation
        Conversation conversation = Conversation.builder()
                // This endpoint only ever creates 1-1 conversations, groups go through createGroup
                .type(ConversationType.DIRECT)
                .participantsHash(userIdsHash)
                .createdDate(Instant.now())
                .modifiedDate(Instant.now())
                .participants(participantInfoList)
                .build();
        conversation = conversationRepository.save(conversation);

        return toConversationResponse(conversation);
    }

    @Override
    public ConversationResponse createGroup(GroupConversationRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        // The creator is added automatically, so drop them (and duplicates / blanks) from the invited list
        Set<String> memberIds = normalizeUserIds(request.getParticipantIds());
        memberIds.remove(userId);
        if (memberIds.isEmpty()) {
            throw new AppException(ErrorCode.GROUP_MEMBERS_REQUIRED);
        }

        // Look everybody up before saving anything, so an unknown user fails the whole request
        List<ParticipantInfo> participants = new ArrayList<>();
        participants.add(toParticipantInfo(fetchProfile(userId)));
        memberIds.forEach(memberId -> participants.add(toParticipantInfo(fetchProfile(memberId))));

        Instant now = Instant.now();
        Conversation conversation = Conversation.builder()
                .type(ConversationType.GROUP)
                .name(request.getName().trim())
                .createdBy(userId)
                .participantsHash("GROUP_" + UUID.randomUUID())
                .participants(participants)
                .createdDate(now)
                .modifiedDate(now)
                .build();
        conversation = conversationRepository.save(conversation);

        return toConversationResponse(conversation);
    }

    @Override
    public ConversationResponse addParticipants(String conversationId, AddParticipantsRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        Conversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_EXISTED));
        // Only members may add people. Non-members get the same answer as for a missing conversation
        boolean isMember = conversation.getParticipants().stream().anyMatch(p -> userId.equals(p.getUserId()));
        if (!isMember) {
            throw new AppException(ErrorCode.CONVERSATION_NOT_EXISTED);
        }
        if (!ConversationType.GROUP.equals(conversation.getType())) {
            throw new AppException(ErrorCode.NOT_GROUP_CONVERSATION);
        }

        // People who are already in the group are skipped, so adding twice is harmless
        Set<String> newMemberIds = normalizeUserIds(request.getParticipantIds());
        conversation.getParticipants().forEach(p -> newMemberIds.remove(p.getUserId()));
        if (newMemberIds.isEmpty()) {
            return toConversationResponse(conversation);
        }

        // Look everybody up before changing anything, so an unknown user fails the whole request
        List<ParticipantInfo> added = newMemberIds.stream()
                .map(this::fetchProfile)
                .map(this::toParticipantInfo)
                .toList();
        List<ParticipantInfo> participants = new ArrayList<>(conversation.getParticipants());
        participants.addAll(added);
        conversation.setParticipants(participants);
        conversation.setModifiedDate(Instant.now());
        conversation = conversationRepository.save(conversation);

        // The cached member list is what grants access to the messages, drop it so new members are recognised now
        cacheService.evict(ChatCacheKeys.participants(conversationId));

        return toConversationResponse(conversation);
    }

    @Override
    public void markSeen(String conversationId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        // Unknown conversation and "not a participant" get the same answer, like everywhere else in this service
        if (!conversationRepository.markSeen(conversationId, userId)) {
            throw new AppException(ErrorCode.CONVERSATION_NOT_EXISTED);
        }
    }

    @Override
    public void updateParticipant(UpdateParticipantRequest request) {
        if (request == null || request.getUserId() == null) return;

        List<Conversation> conversations = conversationRepository.findAllByParticipantIdsContains(request.getUserId());
        for (Conversation conv : conversations) {
            boolean changed = false;
            List<ParticipantInfo> parts = conv.getParticipants();
            if (parts == null) continue;
            for (ParticipantInfo p : parts) {
                if (request.getUserId().equals(p.getUserId())) {
                    if (request.getUsername() != null) p.setUsername(request.getUsername());
                    if (request.getFirstName() != null) p.setFirstName(request.getFirstName());
                    if (request.getLastName() != null) p.setLastName(request.getLastName());
                    if (request.getAvatar() != null) p.setAvatar(request.getAvatar());
                    changed = true;
                }
            }
            if (changed) {
                conv.setModifiedDate(Instant.now());
                conversationRepository.save(conv);
                // Cached messages embed the sender's name/avatar, drop them so they are rebuilt with the new info
                cacheService.evict(ChatCacheKeys.messageIndex(conv.getId()));
                cacheService.evict(ChatCacheKeys.messageData(conv.getId()));
            }
        }
    }

    private String generateParticipantHash(List<String> ids) {
        StringJoiner stringJoiner = new StringJoiner("_");
        ids.forEach(stringJoiner::add);
        return stringJoiner.toString();
    }

    /** Drops null/blank ids and duplicates, keeping the order they were given in. */
    private Set<String> normalizeUserIds(List<String> userIds) {
        Set<String> result = new LinkedHashSet<>();
        if (userIds != null) {
            userIds.stream()
                    .filter(id -> id != null && !id.isBlank())
                    .map(String::trim)
                    .forEach(result::add);
        }
        return result;
    }

    private UserProfileResponse fetchProfile(String userId) {
        try {
            var response = profileClient.getProfile(userId);
            if (Objects.isNull(response) || Objects.isNull(response.getResult())) {
                throw new AppException(ErrorCode.PARTICIPANT_NOT_FOUND);
            }
            return response.getResult();
        } catch (FeignException.NotFound | FeignException.BadRequest e) {
            // profile-service answers 400/404 when it has no profile for this userId
            throw new AppException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }
    }

    private ParticipantInfo toParticipantInfo(UserProfileResponse profile) {
        return ParticipantInfo.builder()
                .userId(profile.getUserId())
                .username(profile.getUsername())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .avatar(profile.getAvatar())
                // Nothing has been sent yet (or, for a new member, nothing they need to be told about)
                .hasSeen(true)
                .build();
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        String currentUserId =
                SecurityContextHolder.getContext().getAuthentication().getName();

        ConversationResponse conversationResponse = conversationMapper.toConversationResponse(conversation);

        if (ConversationType.GROUP.equals(conversation.getType())) {
            // A group is shown with its own name, not with the name of the "other" participant
            conversationResponse.setConversationName(conversation.getName());
            return conversationResponse;
        }

        conversation.getParticipants().stream()
                .filter(participantInfo -> !participantInfo.getUserId().equals(currentUserId))
                .findFirst()
                .ifPresent(participantInfo -> {
                    conversationResponse.setConversationName(participantInfo.getUsername());
                    conversationResponse.setConversationAvatar(participantInfo.getAvatar());
                });

        return conversationResponse;
    }
}
