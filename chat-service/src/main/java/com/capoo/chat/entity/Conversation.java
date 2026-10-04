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
@Document(collection = "conversation")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Conversation {
    @MongoId
    String id;

    String type; // GROUP, DIRECT

    // Group name, null for DIRECT conversations
    String name;

    // userId of whoever created the group, null for DIRECT conversations
    String createdBy;

    // DIRECT: the two sorted userIds. GROUP: "GROUP_<random uuid>", the index is unique so it must never repeat
    @Indexed(unique = true)
    String participantsHash;

    List<ParticipantInfo> participants;

    Instant createdDate;

    Instant modifiedDate;
}
