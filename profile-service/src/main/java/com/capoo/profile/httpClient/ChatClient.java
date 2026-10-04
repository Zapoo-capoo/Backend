package com.capoo.profile.httpClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import com.capoo.dto.ApiResponse;
import com.capoo.profile.configuration.AuthenticationRequestInterceptor;

@FeignClient(
        name = "chat-service",
        url = "http://localhost:8082",
        configuration = {AuthenticationRequestInterceptor.class})
public interface ChatClient {
    @PostMapping("/conversations/participants/update")
    ApiResponse<Boolean> updateParticipant(com.capoo.profile.dto.request.UpdateParticipantRequest request);
}
