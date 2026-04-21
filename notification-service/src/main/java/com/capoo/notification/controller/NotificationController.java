package com.capoo.notification.controller;

import com.capoo.event.dto.NotificationEvent;
import com.capoo.notification.dto.request.EmailUserRequest;
import com.capoo.notification.dto.request.Recipient;
import com.capoo.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationController {
    private final EmailService emailService;
    @KafkaListener(topics = "onboard_successful456")
    public void listen(@Payload NotificationEvent notificationEvent) {
        log.info("Received notification event: {}", notificationEvent);
        emailService.sendEmail(EmailUserRequest.builder()
                .to(Recipient.builder()
                        .email(notificationEvent.getRecipient())
                        .name("Zapoo User")
                        .build())
                .subject(notificationEvent.getSubject())
                .htmlContent(notificationEvent.getBody())
                .build());
    }
}
