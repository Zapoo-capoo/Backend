package com.capoo.profile.controller;

import com.capoo.event.dto.NotificationEvent;
import com.capoo.profile.dto.request.EmailUserRequest;
import com.capoo.profile.dto.request.Recipient;
import com.capoo.profile.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestController;

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
