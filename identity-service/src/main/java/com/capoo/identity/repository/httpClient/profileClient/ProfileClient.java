package com.capoo.identity.repository.httpClient.profileClient;

import com.capoo.identity.dto.request.UserProfileCreationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="profile-service", url = "${app.services.profile.url}")
public interface ProfileClient {
    @PostMapping(value="/user-profiles",produces = MediaType.APPLICATION_JSON_VALUE)
    Object createUserProfileForUser(@RequestBody UserProfileCreationRequest request);
}
