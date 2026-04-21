package com.capoo.notification.controller;

import com.capoo.notification.dto.ApiResponse;
import com.capoo.notification.dto.reponse.EmailReponse;
import com.capoo.notification.dto.request.EmailUserRequest;
import com.capoo.notification.service.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
}
