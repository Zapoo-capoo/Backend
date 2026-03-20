package com.capoo.profile.service;

import com.capoo.profile.dto.reponse.EmailReponse;
import com.capoo.profile.dto.request.EmailRequest;
import com.capoo.profile.dto.request.EmailUserRequest;
import com.capoo.profile.dto.request.Sender;
import com.capoo.profile.exception.AppException;
import com.capoo.profile.exception.ErrorCode;
import com.capoo.profile.repository.httpClient.EmailClient;
import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailService {
    EmailClient emailClient;
    @Value("${brevo.api-key}")
    @NonFinal
    String apiKey;
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
        }catch (FeignException e){
            throw new AppException(ErrorCode.CANNOT_SEND_EMAIL);
        }
    }


}
