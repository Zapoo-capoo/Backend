package com.capoo.chat.dto.response;

import java.time.Instant;
import java.util.List;

import com.capoo.chat.entity.ParticipantInfo;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationResponse {
    String id;
    String type; // GROUP, DIRECT
    String participantsHash;
    String conversationAvatar;
    String conversationName;
    String createdBy;
    List<ParticipantInfo> participants;
    Instant createdDate;
    Instant modifiedDate;
}
