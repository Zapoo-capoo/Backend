package com.capoo.notification.dto.reponse;

import java.time.Instant;
import java.util.Map;

import com.capoo.notification.entity.Notification;
import com.capoo.notification.entity.NotificationType;
import com.capoo.notification.entity.TargetType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {
    String id;
    NotificationType type;
    String actorId;
    String actorName;
    String actorAvatar;
    TargetType targetType;
    String targetId;
    Map<String, Object> data;
    boolean read;
    Instant readAt;
    Instant createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .actorId(n.getActorId())
                .actorName(n.getActorName())
                .actorAvatar(n.getActorAvatar())
                .targetType(n.getTargetType())
                .targetId(n.getTargetId())
                .data(n.getData())
                .read(n.isRead())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
