package com.capoo.chat.service;

import com.capoo.chat.dto.PageResponse;
import com.capoo.chat.dto.request.ChatMessageRequest;
import com.capoo.chat.dto.response.ChatMessageResponse;
import com.capoo.chat.dto.response.FileReponse;
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
import com.capoo.chat.repository.httpclient.FileClient;
import com.capoo.chat.repository.httpclient.ProfileClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    FileClient fileClient;
    public PageResponse<ChatMessageResponse> getMessages(String conversationId, int page, int size) {

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
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");
        Pageable pageable = PageRequest.of(page-1,size,sort);
        Page<ChatMessage> messagePage=chatMessageRepository.findAllByConversationId(conversationId,pageable);
        List<ChatMessageResponse> messageList=messagePage.getContent()
                .stream().map(this::toChatMessageResponse).toList();
        return  PageResponse.<ChatMessageResponse>builder()
                .currentPage(page)
                .pageSize(messagePage.getSize())
                .totalPages(messagePage.getTotalPages())
                .totalElements(messagePage.getTotalElements())
                .data(messageList)
                .build();

    }

    public ChatMessageResponse create(ChatMessageRequest request, MultipartFile file) {
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
        FileReponse response=null;
        if (file != null && !file.isEmpty()) {
            response = fileClient.uploadMedia(file).getResult();
            chatMessage.setImgUrl(response.getUrl());
        }
        //CreateChatMessage
        chatMessageRepository.save(chatMessage);


        //Push message to SocketIO
        //get Participants of conversation
        List<String> participantIds=conversation.getParticipants().stream()
                .map(ParticipantInfo::getUserId)
                .toList();
        //send message to participants via socket
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
            String message;
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
    public void deleteMessage(String messageId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        ChatMessage chatMessage = chatMessageRepository.findById(messageId).orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
        // Only allow sender to delete the message
        if (Objects.isNull(chatMessage.getSender()) || !userId.equals(chatMessage.getSender().getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }
        // Find conversation to get participants for notification
        Conversation conversation = conversationRepository.findById(chatMessage.getConversationId()).orElse(null);
        List<String> participantIds = Collections.emptyList();
        if (conversation != null) {
            participantIds = conversation.getParticipants().stream().map(ParticipantInfo::getUserId).toList();
        }

        // Delete the message
        chatMessageRepository.delete(chatMessage);

        // Notify participants about deletion via socket
        if (!participantIds.isEmpty()) {
            Map<String,WebSocketSession> sessions = webSocketSessionRepository
                    .findAllByUserIdIn(participantIds)
                    .stream()
                    .collect(Collectors.toMap(WebSocketSession::getSocketSessionId, Function.identity()));

            // Build simple deletion payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "delete");
            payload.put("messageId", messageId);
            payload.put("conversationId", chatMessage.getConversationId());

            String message;
            try {
                message = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            final String finalMessage = message;
            socketIOServer.getAllClients().forEach(client -> {
                var webSocketSession = sessions.get(client.getSessionId().toString());
                if (Objects.isNull(webSocketSession)) {
                    return;
                }
                client.sendEvent("message:delete", finalMessage);
            });
        }
    }

    private ChatMessageResponse toChatMessageResponse(ChatMessage chatMessage) {
        ChatMessageResponse chatMessageResponse=chatMessageMapper.toChatMessageResponse(chatMessage);
        String userId=SecurityContextHolder.getContext().getAuthentication().getName();
        chatMessageResponse.setMe(userId.equals(chatMessage.getSender().getUserId()));
        return chatMessageResponse;
    }
}
