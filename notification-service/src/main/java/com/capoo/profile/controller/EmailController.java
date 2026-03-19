package com.capoo.profile.controller;

import com.capoo.profile.dto.ApiResponse;
import com.capoo.profile.dto.reponse.EmailReponse;
import com.capoo.profile.dto.request.EmailUserRequest;
import com.capoo.profile.repository.httpClient.EmailClient;
import com.capoo.profile.service.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
}
