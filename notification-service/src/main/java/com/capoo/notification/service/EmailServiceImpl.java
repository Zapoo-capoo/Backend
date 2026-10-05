package com.capoo.notification.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.capoo.notification.dto.reponse.EmailReponse;
import com.capoo.notification.dto.request.EmailRequest;
import com.capoo.notification.dto.request.EmailUserRequest;
import com.capoo.notification.dto.request.Sender;
import com.capoo.notification.exception.AppException;
import com.capoo.notification.exception.ErrorCode;
import com.capoo.notification.repository.httpClient.EmailClient;

import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailServiceImpl implements EmailService {
    EmailClient emailClient;

    @Value("${brevo.api-key}")
    @NonFinal
    String apiKey;

    @Override
    public EmailReponse sendEmail(EmailUserRequest request) {
        EmailRequest emailRequest = EmailRequest.builder()
                .sender(Sender.builder()
                        .name("Zapoo")
                        .email("tuanvip069@gmail.com")
                        .build())
                .to(List.of(request.getTo()))
                .subject(request.getSubject())
                .htmlContent(request.getHtmlContent())
                .build();
        try {
            return emailClient.sendEmail(apiKey, emailRequest);
        } catch (FeignException e) {
            // Brevo explains the refusal in the response body (invalid key, sender not validated, IP not allowed...)
            log.error("Brevo refused the email to {}: HTTP {} {}", request.getTo().getEmail(), e.status(), e.contentUTF8());
            throw new AppException(ErrorCode.CANNOT_SEND_EMAIL);
        }
    }
}
