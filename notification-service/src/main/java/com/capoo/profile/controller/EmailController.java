package com.capoo.profile.controller;

import com.capoo.profile.dto.ApiResponse;
import com.capoo.profile.dto.reponse.EmailReponse;
import com.capoo.profile.dto.request.EmailUserRequest;
import com.capoo.profile.service.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailController {
    EmailService emailService;
    @PostMapping("/email/send")
    ApiResponse<EmailReponse> sendEmail(
            @RequestBody EmailUserRequest body
    ) {
        return ApiResponse.<EmailReponse>builder()
                .result(emailService.sendEmail(body))
                .build();
    }
    @KafkaListener(topics = "onboard-successful", groupId = "notification-group-test-123")    public void listen(ConsumerRecord<String, String> record) {
        // Log value plus metadata to make diagnosis easier
        log.info("Received message: {} (topic={}, partition={}, offset={})",
                record.value(), record.topic(), record.partition(), record.offset());
        // You can also call emailService.sendEmail(...) here if the payload matches the request model
    }
}
