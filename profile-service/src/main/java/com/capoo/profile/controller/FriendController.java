package com.capoo.profile.controller;

import com.capoo.profile.dto.ApiResponse;
import com.capoo.profile.dto.response.FriendRequestReponse;
import com.capoo.profile.dto.response.UserProfileReponse;
import com.capoo.profile.entity.UserProfile;
import com.capoo.profile.service.FriendService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class FriendController {
    FriendService service;
    @PostMapping("/request")
    public ApiResponse<FriendRequestReponse> sendFriendRequest(@RequestParam(name = "username") String username) {
        FriendRequestReponse requestReponse=service.sendRequest(username);
        return ApiResponse.<FriendRequestReponse>builder()
                .result(requestReponse)
                .build();
    }

    @PostMapping("/reject")
    public ApiResponse<FriendRequestReponse> reject(@RequestParam(name = "id") String toId) {
        FriendRequestReponse requestReponse= service.reject(toId);
        return ApiResponse.<FriendRequestReponse>builder()
                .result(requestReponse)
                .build();
    }

    @PostMapping("/unfriend")
    public ApiResponse<FriendRequestReponse> unfriend(@RequestParam(name = "id") String toId) {
        FriendRequestReponse requestReponse= service.unfriend(toId);
        return ApiResponse.<FriendRequestReponse>builder()
                .result(requestReponse)
                .build();
    }
    @GetMapping("/sent")
    public ApiResponse<List<UserProfileReponse>> getSent() {
        return ApiResponse.<List<UserProfileReponse>>builder()
                .result(service.getSentRequests())
                .build();
    }
    @GetMapping("/received")
    public ApiResponse<List<UserProfileReponse>> getReceived() {
        return ApiResponse.<List<UserProfileReponse>>builder()
                .result(service.getReceivedRequests())
                .build();
    }
    @GetMapping("/friends")
    public ApiResponse<List<UserProfileReponse>> getAllFriend() {
        return ApiResponse.<List<UserProfileReponse>>builder()
                .result(service.getAllFriendRequests())
                .build();
    }
    @GetMapping("/{userId}/friends")
    public ApiResponse<List<UserProfileReponse>> getAllFriendById(@PathVariable String userId) {
        return ApiResponse.<List<UserProfileReponse>>builder()
                .result(service.getAllFriendRequestsById(userId))
                .build();
    }

}