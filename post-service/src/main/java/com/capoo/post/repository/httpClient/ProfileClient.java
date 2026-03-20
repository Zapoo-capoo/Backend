package com.capoo.post.repository.httpClient;

import com.capoo.post.configuration.AuthenticationRequestInterceptor;
import com.capoo.post.dto.ApiResponse;
import com.capoo.post.dto.response.UserProfileReponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "profile-service", url = "${app.services.profile}",
        configuration = {AuthenticationRequestInterceptor.class})
public interface ProfileClient {
    @GetMapping("/internal/users/{userId}")
    ApiResponse<UserProfileReponse> getUserProfileByUserId(@PathVariable String userId);
}
