package com.capoo.chat.service;

import static com.capoo.chat.service.cache.ChatCacheKeys.MESSAGE_CACHE_SIZE;
import static com.capoo.chat.service.cache.ChatCacheKeys.TTL;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.capoo.chat.dto.CursorResponse;
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
import com.capoo.chat.service.cache.ChatCacheKeys;
import com.capoo.chat.service.cache.ICacheService;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatMessageServiceImpl implements ChatMessageService {
    private static final int MAX_PAGE_SIZE = 50;
    // Epoch millis are always positive, so 0 is a safe lower bound for score queries
    private static final double MIN_SCORE = 0;
    private static final Comparator<ChatMessageResponse> NEWEST_FIRST = Comparator.comparingLong(
                    (ChatMessageResponse m) -> m.getCreatedDate().toEpochMilli())
            .thenComparing(ChatMessageResponse::getId)
            .reversed();

    ChatMessageRepository chatMessageRepository;
    ProfileClient profileClient;
    SocketIOServer socketIOServer;
    WebSocketSessionRepository webSocketSessionRepository;
    ObjectMapper objectMapper;
    ChatMessageMapper chatMessageMapper;
    ConversationRepository conversationRepository;
    FileClient fileClient;
    ICacheService cacheService;

    /**
     * Cursor pagination, newest message first. The newest {@value ChatCacheKeys#MESSAGE_CACHE_SIZE} messages of a
     * conversation are cached in a ZSET (score = createdDate); anything the cache cannot answer completely is read
     * from MongoDB.
     */
    @Override
    public CursorResponse<ChatMessageResponse> getMessages(String conversationId, String cursor, int size) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        int pageSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        MessageCursor messageCursor = MessageCursor.parse(cursor);

        // Membership is checked before any cache lookup so cached messages are never served to non-members
        if (!getParticipantIds(conversationId).contains(userId)) {
            throw new AppException(ErrorCode.CONVERSATION_NOT_EXISTED);
        }

        // Read one extra message to know whether another page exists
        List<ChatMessageResponse> messages = readFromCache(conversationId, messageCursor, pageSize + 1);
        if (messages == null) {
            messages = readFromDatabase(conversationId, messageCursor, pageSize + 1);
        }

        boolean hasMore = messages.size() > pageSize;
        List<ChatMessageResponse> page = new ArrayList<>(messages.subList(0, Math.min(pageSize, messages.size())));

        return CursorResponse.<ChatMessageResponse>builder()
                .size(page.size())
                .hasMore(hasMore)
                .nextCursor(hasMore ? MessageCursor.of(page.getLast()).format() : null)
                .data(page)
                .build();
    }

    @Override
    public ChatMessageResponse create(ChatMessageRequest request, MultipartFile file) {
        // validate conversationId
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Conversation conversation = conversationRepository
                .findById(request.getConversationId())
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_EXISTED));
        // Check if user is participant of conversation
        conversation.getParticipants().stream()
                .filter(participant -> participant.getUserId().equals(userId))
                .findAny()
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_EXISTED));
        // GetUserInfoUs
        var userProfileResponse = profileClient.getProfile(userId);
        if (Objects.isNull(userProfileResponse)) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        UserProfileResponse userInfo = userProfileResponse.getResult();
        // Build ChatMessage
        ChatMessage chatMessage = chatMessageMapper.toChatMessage(request);
        chatMessage.setSender(ParticipantInfo.builder()
                .userId(userId)
                .username(userInfo.getUsername())
                .firstName(userInfo.getFirstName())
                .lastName(userInfo.getLastName())
                .avatar(userInfo.getAvatar())
                .build());
        // MongoDB keeps millisecond precision, the cache score and the cursor are millisecond based as well
        chatMessage.setCreatedDate(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        FileReponse response = null;
        if (file != null && !file.isEmpty()) {
            response = fileClient.uploadMedia(file).getResult();
            chatMessage.setImgUrl(response.getUrl());
        }
        // CreateChatMessage
        chatMessageRepository.save(chatMessage);
        // Everybody but the sender now has an unseen message in this conversation
        conversationRepository.markUnread(conversation.getId(), userId);
        ChatMessageResponse chatMessageResponse = chatMessageMapper.toChatMessageResponse(chatMessage);

        addToCache(chatMessage.getConversationId(), chatMessageResponse);

        // Push message to SocketIO
        // get Participants of conversation
        List<String> participantIds = conversation.getParticipants().stream()
                .map(ParticipantInfo::getUserId)
                .toList();
        // send message to participants via socket
        Map<String, WebSocketSession> sessions = webSocketSessionRepository.findAllByUserIdIn(participantIds).stream()
                .collect(Collectors.toMap(WebSocketSession::getSocketSessionId, Function.identity()));
        String message;

        try {
            message = objectMapper.writeValueAsString(chatMessageResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        for (String id : participantIds) {
            socketIOServer.getRoomOperations("user-" + id).sendEvent("message", message);
        }
        //        socketIOServer.getAllClients().forEach(client -> {
        //            var webSocketSession=sessions.get(client.getSessionId().toString());
        //            if (Objects.isNull(webSocketSession)) {
        //                return;
        //            }
        //            client.sendEvent("message", message);
        //        });
        return chatMessageResponse;
    }

    @Override
    public void deleteMessage(String messageId) {

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        ChatMessage chatMessage = chatMessageRepository
                .findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
        // Only allow sender to delete the message
        if (Objects.isNull(chatMessage.getSender())
                || !userId.equals(chatMessage.getSender().getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }
        // Find conversation to get participants for notification
        Conversation conversation =
                conversationRepository.findById(chatMessage.getConversationId()).orElse(null);
        List<String> participantIds = Collections.emptyList();
        if (conversation != null) {
            participantIds = conversation.getParticipants().stream()
                    .map(ParticipantInfo::getUserId)
                    .toList();
        }

        // Delete the message
        chatMessageRepository.delete(chatMessage);
        // Remove from cache if exists
        removeFromCache(chatMessage.getConversationId(), messageId);

        // Notify participants about deletion via socket
        if (!participantIds.isEmpty()) {
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

            // Same delivery as the "message" event: every participant has a room, no session lookup needed
            for (String id : participantIds) {
                socketIOServer.getRoomOperations("user-" + id).sendEvent("message:delete", message);
            }
        }
    }

    /* ---------------------------------------------- cache helpers ---------------------------------------------- */

    /** Participant ids of a conversation (cached), throws if the conversation does not exist. */
    @SuppressWarnings("unchecked")
    private List<String> getParticipantIds(String conversationId) {
        String key = ChatCacheKeys.participants(conversationId);
        Optional<List> cached = cacheService.get(key, List.class);
        if (cached.isPresent()) {
            return (List<String>) cached.get();
        }
        Conversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_EXISTED));
        List<String> ids = conversation.getParticipants().stream()
                .map(ParticipantInfo::getUserId)
                .collect(Collectors.toCollection(ArrayList::new));
        cacheService.set(key, ids, TTL);
        return ids;
    }

    /**
     * Messages older than the cursor, newest first (at most {@code limit}), or {@code null} when the cache cannot
     * answer completely and the database has to be used. The cache always holds the newest
     * {@value ChatCacheKeys#MESSAGE_CACHE_SIZE} messages, so a cache with fewer entries holds the whole conversation.
     */
    private List<ChatMessageResponse> readFromCache(String conversationId, MessageCursor cursor, int limit) {
        String indexKey = ChatCacheKeys.messageIndex(conversationId);
        String dataKey = ChatCacheKeys.messageData(conversationId);
        if (!cacheService.exists(indexKey)) {
            return null;
        }

        // Inclusive upper bound, messages created in the same millisecond as the cursor are filtered below
        double max = cursor == null ? Double.MAX_VALUE : cursor.millis();
        List<String> ids =
                cacheService.zReverseRangeByScore(indexKey, max, MIN_SCORE, 0, MESSAGE_CACHE_SIZE, String.class);
        List<ChatMessageResponse> cached =
                ids.isEmpty() ? List.of() : cacheService.hMGet(dataKey, ids, ChatMessageResponse.class);
        // Not cached.contains(null): that throws NullPointerException on the immutable List.of()
        if (cached.size() != ids.size() || cached.stream().anyMatch(Objects::isNull)) {
            // Index and data went out of sync (for example one of them expired first), rebuild from the database
            evictMessages(conversationId);
            return null;
        }

        List<ChatMessageResponse> older = cached.stream()
                .filter(message -> cursor == null || cursor.isBefore(message))
                .sorted(NEWEST_FIRST)
                .collect(Collectors.toCollection(ArrayList::new));
        boolean cacheHoldsWholeConversation = cacheService.zCard(indexKey) < MESSAGE_CACHE_SIZE;
        if (older.size() < limit && !cacheHoldsWholeConversation) {
            return null;
        }

        cacheService.expire(indexKey, TTL);
        cacheService.expire(dataKey, TTL);
        return new ArrayList<>(older.subList(0, Math.min(limit, older.size())));
    }

    private List<ChatMessageResponse> readFromDatabase(String conversationId, MessageCursor cursor, int limit) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate").and(Sort.by(Sort.Direction.DESC, "id"));

        if (cursor == null) {
            // First page: also (re)build the cache with the newest messages
            List<ChatMessage> newest = chatMessageRepository.findByConversationId(
                    conversationId, PageRequest.of(0, Math.max(limit, MESSAGE_CACHE_SIZE), sort));
            fillCache(conversationId, newest.stream().limit(MESSAGE_CACHE_SIZE).toList());
            return newest.stream()
                    .limit(limit)
                    .map(chatMessageMapper::toChatMessageResponse)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        // Strictly before the cursor = older createdDate, or the same createdDate with a smaller id
        Instant cursorTime = Instant.ofEpochMilli(cursor.millis());
        List<ChatMessage> sameInstant =
                chatMessageRepository.findByConversationIdAndCreatedDate(conversationId, cursorTime).stream()
                        .filter(message -> message.getId().compareTo(cursor.id()) < 0)
                        .sorted(Comparator.comparing(ChatMessage::getId).reversed())
                        .toList();
        List<ChatMessage> older = chatMessageRepository.findByConversationIdAndCreatedDateLessThan(
                conversationId, cursorTime, PageRequest.of(0, limit, sort));
        return Stream.concat(sameInstant.stream(), older.stream())
                .limit(limit)
                .map(chatMessageMapper::toChatMessageResponse)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /** Replaces the cached messages of a conversation with the given (newest) messages. */
    private void fillCache(String conversationId, List<ChatMessage> newest) {
        evictMessages(conversationId);
        if (newest.isEmpty()) {
            return;
        }
        String indexKey = ChatCacheKeys.messageIndex(conversationId);
        Map<String, ChatMessageResponse> data = new LinkedHashMap<>();
        Map<Object, Double> scores = new LinkedHashMap<>();
        for (ChatMessage message : newest) {
            data.put(message.getId(), chatMessageMapper.toChatMessageResponse(message));
            scores.put(message.getId(), (double) message.getCreatedDate().toEpochMilli());
        }
        // Data first, so a reader that sees the index entry always finds the message body
        cacheService.hMSet(ChatCacheKeys.messageData(conversationId), data, TTL);
        cacheService.zAddAll(indexKey, scores);
        cacheService.expire(indexKey, TTL);
    }

    /** Adds a new message to an existing cache. A missing cache is left alone and rebuilt on the next read. */
    private void addToCache(String conversationId, ChatMessageResponse message) {
        String indexKey = ChatCacheKeys.messageIndex(conversationId);
        if (!cacheService.exists(indexKey)) {
            return;
        }
        String dataKey = ChatCacheKeys.messageData(conversationId);
        cacheService.hSet(dataKey, message.getId(), message);
        cacheService.zAdd(indexKey, message.getId(), message.getCreatedDate().toEpochMilli());

        // Keep only the newest MESSAGE_CACHE_SIZE messages
        long size = cacheService.zCard(indexKey);
        if (size > MESSAGE_CACHE_SIZE) {
            List<String> oldest = cacheService.zRange(indexKey, 0, size - MESSAGE_CACHE_SIZE - 1, String.class);
            if (!oldest.isEmpty()) {
                cacheService.zRemove(indexKey, oldest.toArray());
                cacheService.hDelete(dataKey, oldest.toArray());
            }
        }
        cacheService.expire(indexKey, TTL);
        cacheService.expire(dataKey, TTL);
    }

    private void removeFromCache(String conversationId, String messageId) {
        String indexKey = ChatCacheKeys.messageIndex(conversationId);
        if (cacheService.zScore(indexKey, messageId) == null) {
            // Not cached: older than the cached range, or no cache at all
            return;
        }
        if (cacheService.zCard(indexKey) >= MESSAGE_CACHE_SIZE) {
            // The cache would stop holding the newest messages, rebuild it on the next read
            evictMessages(conversationId);
            return;
        }
        cacheService.zRemove(indexKey, messageId);
        cacheService.hDelete(ChatCacheKeys.messageData(conversationId), messageId);
    }

    private void evictMessages(String conversationId) {
        cacheService.evict(ChatCacheKeys.messageIndex(conversationId));
        cacheService.evict(ChatCacheKeys.messageData(conversationId));
    }

    /** Position of a message in the (createdDate, id) ordering, formatted as {@code <epochMillis>_<messageId>}. */
    private record MessageCursor(long millis, String id) {
        static MessageCursor parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            int separator = raw.indexOf('_');
            if (separator <= 0 || separator == raw.length() - 1) {
                throw new AppException(ErrorCode.INVALID_CURSOR);
            }
            try {
                return new MessageCursor(Long.parseLong(raw.substring(0, separator)), raw.substring(separator + 1));
            } catch (NumberFormatException e) {
                throw new AppException(ErrorCode.INVALID_CURSOR);
            }
        }

        static MessageCursor of(ChatMessageResponse message) {
            return new MessageCursor(message.getCreatedDate().toEpochMilli(), message.getId());
        }

        String format() {
            return millis + "_" + id;
        }

        /** True if this cursor comes strictly before the message in newest-first order (i.e. the message is older). */
        boolean isBefore(ChatMessageResponse message) {
            long messageMillis = message.getCreatedDate().toEpochMilli();
            return messageMillis < millis
                    || (messageMillis == millis && message.getId().compareTo(id) < 0);
        }
    }
}
