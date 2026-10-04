package com.capoo.notification.service;

import com.capoo.notification.dto.reponse.EmailReponse;
import com.capoo.notification.dto.request.EmailUserRequest;

public interface EmailService {
    EmailReponse sendEmail(EmailUserRequest request);
}
