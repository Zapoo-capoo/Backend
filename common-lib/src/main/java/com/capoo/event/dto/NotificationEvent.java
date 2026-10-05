package com.capoo.event.dto;

import java.util.List;
import java.util.Map;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Event of the notification pipeline. Two shapes share this class:
 * <ul>
 *   <li>e-mail events (identity-service, topic {@code onboard_successful456}): channel, recipient, subject, body...</li>
 *   <li>in-app events (topic app.kafka.topics.notification-events): type, actor*, recipientIds, target*, data</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationEvent {
    // ---- e-mail events
    String channel;
    String recipient;
    String templateCode;
    Map<String, Object> param;
    String subject;
    String body;

    // ---- in-app events
    /** FRIEND_REQUEST, FRIEND_ACCEPTED, NEW_POST... (a String so this module does not depend on the enum) */
    String type;

    /** userId of whoever caused the event, null for system notifications */
    String actorId;

    String actorName;
    String actorAvatar;

    /** userIds of the people to notify */
    List<String> recipientIds;

    /** POST, USER or NONE: what the client opens when the notification is tapped */
    String targetType;

    String targetId;
    Map<String, Object> data;
}
