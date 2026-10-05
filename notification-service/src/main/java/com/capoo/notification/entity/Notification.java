package com.capoo.notification.entity;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
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
@Document(collection = "notifications")
@CompoundIndexes({
    // Notification list of one user, newest first (cursor pagination on createdAt)
    @CompoundIndex(name = "recipient_created", def = "{'recipientId': 1, 'createdAt': -1}"),
    // Unread counter
    @CompoundIndex(name = "recipient_read", def = "{'recipientId': 1, 'read': 1}"),
    // The same event never creates two notifications for the same user
    @CompoundIndex(name = "recipient_dedup", def = "{'recipientId': 1, 'dedupKey': 1}", unique = true)
})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {
    @MongoId
    String id;

    // userId (the "sub" claim of the JWT), not the profile id
    String recipientId;

    NotificationType type;

    // Who caused the event, null for system notifications
    String actorId;

    // Snapshot of the actor, so the list does not need a call to profile-service
    String actorName;

    String actorAvatar;

    // Tells the client where to go when the notification is opened
    TargetType targetType;

    // postId when targetType is POST, the actor's userId when it is USER
    String targetId;

    // Extra data, for example {"postPreview": "..."}
    Map<String, Object> data;

    boolean read;

    Instant readAt;

    // TTL index: notifications are removed after 90 days (needs spring.data.mongodb.auto-index-creation=true)
    @Indexed(expireAfterSeconds = 7_776_000)
    Instant createdAt;

    // Identifies the event, like FRIEND_REQUEST:<fromUserId> or NEW_POST:<postId>
    String dedupKey;
}
