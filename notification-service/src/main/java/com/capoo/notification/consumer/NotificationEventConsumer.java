package com.capoo.notification.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.capoo.event.dto.NotificationEvent;
import com.capoo.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** In-app notification events from the other services (friend requests, accepted requests...) */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {
    private final NotificationService notificationService;

    @KafkaListener(topics = "${app.kafka.topics.notification-events:notification.events}")
    public void onEvent(@Payload NotificationEvent event) {
        log.info("Received {} event from {} for {}", event.getType(), event.getActorId(), event.getRecipientIds());
        notificationService.createFromEvent(event);
    }
}
