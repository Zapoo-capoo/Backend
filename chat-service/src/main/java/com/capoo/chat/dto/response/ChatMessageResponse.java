package com.capoo.chat.dto.response;

import java.time.Instant;

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
    String imgUrl;
    ParticipantInfo sender;
    Instant createdDate;
}
