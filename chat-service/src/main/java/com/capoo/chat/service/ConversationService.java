package com.capoo.chat.service;

import com.capoo.chat.dto.request.ConversationRequest;
import com.capoo.chat.dto.response.ConversationResponse;
import com.capoo.chat.entity.Conversation;
import com.capoo.chat.entity.ParticipantInfo;
import com.capoo.chat.mapper.ConversationMapper;
import com.capoo.chat.repository.ConversationRepository;
import com.capoo.chat.repository.httpclient.ProfileClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationService {
    ConversationRepository conversationRepository;
    ProfileClient profileClient;

    ConversationMapper conversationMapper;

    public List<ConversationResponse> myConversations() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Conversation> conversations = conversationRepository.findAllByParticipantIdsContains(userId);
        return conversations.stream().map(this::toConversationResponse).toList();
    }

    public ConversationResponse create(ConversationRequest request) {
        //Fetch user info
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        var userProfileResponse = profileClient.getProfile(userId);
        var participantInfoResponses = profileClient.getProfile(
                request.getParticipantIds().getFirst());
        if (Objects.isNull(participantInfoResponses)||Objects.isNull(participantInfoResponses.getResult())) {
            throw new RuntimeException("Participant not found");
        }
        var userInfo = userProfileResponse.getResult();
        var participantInfo = participantInfoResponses.getResult();

        List<String> userIds=new ArrayList<>();
        userIds.add(userId);
        userIds.add(request.getParticipantIds().getFirst());

        var sortedId=userIds.stream().sorted().toList();
        String userIdsHash=generateParticipantHash(sortedId);

        var conversationOptional=conversationRepository.findByParticipantsHash(userIdsHash);
        if(conversationOptional.isPresent()){
            return toConversationResponse(conversationOptional.get());
        }

        List<ParticipantInfo>  participantInfoList= List.of(
          ParticipantInfo.builder()
                  .userId(userInfo.getUserId())
                  .username(userInfo.getUsername())
                  .firstName(userInfo.getFirstName())
                  .lastName(userInfo.getLastName())
                  .avatar(userInfo.getAvatar())
                    .build(),
            ParticipantInfo.builder()
                    .userId(participantInfo.getUserId())
            .username(participantInfo.getUsername())
            .firstName(participantInfo.getFirstName())
            .lastName(participantInfo.getLastName())
            .avatar(participantInfo.getAvatar())
            .build()
        );
        //Build conversation
        Conversation conversation=Conversation.builder()
                .type(request.getType())
                .participantsHash(userIdsHash)
                .createdDate(Instant.now())
                .modifiedDate(Instant.now())
                .participants(participantInfoList)
                .build();
        conversation = conversationRepository.save(conversation);

        return toConversationResponse(conversation);
    }

    private String generateParticipantHash(List<String> ids) {
        StringJoiner stringJoiner = new StringJoiner("_");
        ids.forEach(stringJoiner::add);
        return stringJoiner.toString();
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        ConversationResponse conversationResponse = conversationMapper.toConversationResponse(conversation);

        conversation.getParticipants().stream()
                .filter(participantInfo -> !participantInfo.getUserId().equals(currentUserId))
                .findFirst().ifPresent(participantInfo -> {
                    conversationResponse.setConversationName(participantInfo.getUsername());
                    conversationResponse.setConversationAvatar(participantInfo.getAvatar());
                });

        return conversationResponse;
    }
}
