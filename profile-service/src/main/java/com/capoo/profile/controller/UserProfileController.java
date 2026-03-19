package com.capoo.profile.controller;

import com.capoo.profile.dto.ApiResponse;
import com.capoo.profile.dto.request.UserProfileCreationRequest;
import com.capoo.profile.dto.response.UserProfileReponse;
import com.capoo.profile.entity.UserProfile;
import com.capoo.profile.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user-profiles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserProfileController {
    UserProfileService userProfileService;
    @GetMapping("/{profileId}")
    ApiResponse<UserProfileReponse> getUserProfile(@PathVariable String profileId) {
        return ApiResponse.<UserProfileReponse>builder()
                .result(userProfileService.getUserProfile(profileId))
                .build();
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping()
    ApiResponse<List<UserProfileReponse>> getAllUserProfiles() {
        return ApiResponse.<List<UserProfileReponse>>builder()
                .result(userProfileService.getUserProfiles())
                .build();
    }


}
