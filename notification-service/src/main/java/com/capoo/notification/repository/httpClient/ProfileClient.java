package com.capoo.notification.repository.httpClient;

import com.capoo.notification.dto.ApiResponse;
import com.capoo.notification.dto.reponse.UserProfileReponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "profile-service", url = "${app.services.profile}")
public interface ProfileClient {
    @GetMapping("/internal/users/{userId}")
    ApiResponse<UserProfileReponse> getUserProfileByUserId(@PathVariable String userId);


    @GetMapping("/friends/{userId}/friends")
    ApiResponse<List<UserProfileReponse>>  getAllFriendById(@PathVariable String userId);
}
