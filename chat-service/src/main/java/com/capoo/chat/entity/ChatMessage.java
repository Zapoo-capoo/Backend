package com.capoo.chat.entity;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_message")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessage {
    @MongoId
    String id;

    @Indexed
    String conversationId;

    @Indexed
    Instant createdDate;

    String message;

    // Kept for messages sent before attachments existed (and mirrors the first image of newer ones)
    String imgUrl;

    // Images and videos of the message, at most 5. Null on messages sent before attachments existed
    List<Attachment> attachments;

    ParticipantInfo sender;
}
