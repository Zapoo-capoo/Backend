package com.capoo.notification.service;

import com.capoo.event.dto.NotificationEvent;
import com.capoo.notification.dto.reponse.CursorResponse;
import com.capoo.notification.dto.reponse.NotificationResponse;

public interface NotificationService {
    void handlePostCreatedEvent(String userId);

    /** Stores one in-app notification per recipient of the event (FRIEND_REQUEST, FRIEND_ACCEPTED...) */
    void createFromEvent(NotificationEvent event);

    /** Notifications of the current user, newest first. {@code cursor} is the nextCursor of the previous page */
    CursorResponse<NotificationResponse> getMyNotifications(String cursor, int size, boolean unreadOnly);

    long countMyUnread();

    void markAsRead(String notificationId);

    void markAllAsRead();
}
