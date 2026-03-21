package com.capoo.chat.service;

import com.capoo.chat.dto.request.ChatMessageRequest;
import com.capoo.chat.dto.response.ChatMessageResponse;
import com.capoo.chat.dto.response.UserProfileResponse;
import com.capoo.chat.entity.ChatMessage;
import com.capoo.chat.entity.Conversation;
import com.capoo.chat.entity.ParticipantInfo;
import com.capoo.chat.entity.WebSocketSession;
import com.capoo.chat.exception.AppException;
import com.capoo.chat.exception.ErrorCode;
import com.capoo.chat.mapper.ChatMessageMapper;
import com.capoo.chat.repository.ChatMessageRepository;
import com.capoo.chat.repository.ConversationRepository;
import com.capoo.chat.repository.WebSocketSessionRepository;
import com.capoo.chat.repository.httpclient.ProfileClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatMessageService {
    ChatMessageRepository chatMessageRepository;
    ProfileClient profileClient;
    SocketIOServer socketIOServer;
    WebSocketSessionRepository webSocketSessionRepository;
    ObjectMapper objectMapper;
    ChatMessageMapper chatMessageMapper;
    ConversationRepository conversationRepository;
    public List<ChatMessageResponse> getMessages(String conversationId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Conversation conversation=conversationRepository.findById(conversationId).orElseThrow(()->new AppException(ErrorCode.CONVERSATION_NOT_EXISTED));
        //Check if user is participant of conversation
        conversation.getParticipants().stream()
                .filter(participant -> participant.getUserId().equals(userId))
                .findAny().orElseThrow(()->new AppException(ErrorCode.CONVERSATION_NOT_EXISTED));
        //GetUserInfoUs
        var userProfileResponse=profileClient.getProfile(userId);
        if (Objects.isNull(userProfileResponse)) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        var messages=chatMessageRepository.findAllByConversationIdOrderByCreatedDateDesc(conversationId);
        return messages.stream()
                .map(this::toChatMessageResponse)
                .toList();
    }

    public ChatMessageResponse create(ChatMessageRequest request) throws JsonProcessingException {
        //validate conversationId
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Conversation conversation=conversationRepository.findById(request.getConversationId()).orElseThrow(()->new AppException(ErrorCode.CONVERSATION_NOT_EXISTED));
        //Check if user is participant of conversation
        conversation.getParticipants().stream()
                .filter(participant -> participant.getUserId().equals(userId))
                .findAny().orElseThrow(()->new AppException(ErrorCode.CONVERSATION_NOT_EXISTED));
        //GetUserInfoUs
        var userProfileResponse=profileClient.getProfile(userId);
        if (Objects.isNull(userProfileResponse)) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        UserProfileResponse userInfo= userProfileResponse.getResult();
        //Build ChatMessage
        ChatMessage chatMessage=chatMessageMapper.toChatMessage(request);
        chatMessage.setSender(
                ParticipantInfo.builder()
                        .userId(userId)
                        .username(userInfo.getUsername())
                        .firstName(userInfo.getFirstName())
                        .lastName(userInfo.getLastName())
                        .avatar(userInfo.getAvatar())
                        .build()
        );
        chatMessage.setCreatedDate(Instant.now());
        //CreateChatMessage
        var chat=chatMessageRepository.save(chatMessage);
        //Push message to SocketIO
        //get Participants of conversation
        List<String> participantIds=conversation.getParticipants().stream()
                .map(ParticipantInfo::getUserId)
                .toList();
        Map<String,WebSocketSession> sessions=webSocketSessionRepository
                .findAllByUserIdIn(participantIds)
                        .stream()
                        .collect(Collectors.toMap(WebSocketSession::getSocketSessionId, Function.identity()));
        ChatMessageResponse chatMessageResponse=chatMessageMapper.toChatMessageResponse(chatMessage);
        socketIOServer.getAllClients().forEach(client -> {
            var webSocketSession=sessions.get(client.getSessionId().toString());
            if (Objects.isNull(webSocketSession)) {
                return;
            }
            String message= null;
            try {
                chatMessageResponse.setMe(webSocketSession.getUserId().equals(userInfo.getUserId()));
                message = objectMapper.writeValueAsString(chatMessageResponse);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            client.sendEvent("message", message);
        });

        return toChatMessageResponse(chatMessage);
    }
    private ChatMessageResponse toChatMessageResponse(ChatMessage chatMessage) {
        ChatMessageResponse chatMessageResponse=chatMessageMapper.toChatMessageResponse(chatMessage);
        String userId=SecurityContextHolder.getContext().getAuthentication().getName();
        chatMessageResponse.setMe(userId.equals(chatMessage.getSender().getUserId()));
        return chatMessageResponse;
    }
}
