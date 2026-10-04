package com.capoo.chat.dto.response;

import java.time.Instant;
import java.util.List;

import com.capoo.chat.entity.Attachment;
import com.capoo.chat.entity.ParticipantInfo;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessageResponse {
    String id;
    String conversationId;
    String message;
    // Deprecated: the first image of the message, kept for older clients. Use attachments
    String imgUrl;
    // Images and videos of the message, empty when there are none
    List<Attachment> attachments;
    ParticipantInfo sender;
    Instant createdDate;
}
