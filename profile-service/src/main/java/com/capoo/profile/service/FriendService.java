package com.capoo.profile.service;

import java.util.List;

import com.capoo.profile.dto.response.FriendRequestReponse;
import com.capoo.profile.dto.response.UserProfileReponse;

public interface FriendService {

    FriendRequestReponse sendRequest(String username);

    FriendRequestReponse reject(String toProfileId);

    FriendRequestReponse unfriend(String toProfileId);

    List<UserProfileReponse> getSentRequests();

    List<UserProfileReponse> getReceivedRequests();

    List<UserProfileReponse> getAllFriendRequests();

    List<UserProfileReponse> getAllFriendRequestsById(String profileId);
}
