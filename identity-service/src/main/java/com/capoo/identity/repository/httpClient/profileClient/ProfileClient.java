package com.capoo.identity.repository.httpClient.profileClient;

import com.capoo.identity.configuration.AuthenticationRequestInterceptor;
import com.capoo.event.dto.UserProfileCreationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="profile-service", url = "${app.services.profile.url}",
    configuration = {AuthenticationRequestInterceptor.class})
public interface ProfileClient {
    @PostMapping(value="/internal/users",produces = MediaType.APPLICATION_JSON_VALUE)
    Object createUserProfileForUser(
            @RequestBody UserProfileCreationRequest request);

    @PutMapping(value = "/users/my-profile", produces = MediaType.APPLICATION_JSON_VALUE)
    Object updateMyProfile(@RequestBody com.capoo.identity.dto.request.UpdateProfileRequest request);
}
