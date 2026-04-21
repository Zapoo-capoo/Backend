package com.capoo.notification.repository.httpClient;

import com.capoo.notification.dto.reponse.EmailReponse;
import com.capoo.notification.dto.request.EmailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name="email-service", url = "https://api.brevo.com")
public interface EmailClient {
    @PostMapping(value = "/v3/smtp/email",produces = MediaType.APPLICATION_JSON_VALUE)
    EmailReponse sendEmail(@RequestHeader("api-key") String apiKey, @RequestBody EmailRequest body);
}
