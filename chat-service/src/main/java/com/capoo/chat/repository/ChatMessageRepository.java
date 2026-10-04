package com.capoo.chat.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.capoo.chat.entity.ChatMessage;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findAllByConversationIdOrderByCreatedDateDesc(String conversationId);

    Page<ChatMessage> findAllByConversationId(String conversationId, Pageable pageable);

    // Cursor pagination: returns a plain List so Spring Data does not run an extra count query
    List<ChatMessage> findByConversationId(String conversationId, Pageable pageable);

    List<ChatMessage> findByConversationIdAndCreatedDateLessThan(
            String conversationId, Instant createdDate, Pageable pageable);

    List<ChatMessage> findByConversationIdAndCreatedDate(String conversationId, Instant createdDate);
}
