package com.capoo.identity.service;

import java.util.List;

import com.capoo.identity.dto.request.PasswordCreationRequest;
import com.capoo.identity.dto.request.UserCreationRequest;
import com.capoo.identity.dto.request.UserUpdateRequest;
import com.capoo.identity.dto.response.UserResponse;

public interface UserService {
    UserResponse createUser(UserCreationRequest userCreationRequest);

    UserResponse updateUser(String userId, UserUpdateRequest request);

    void deleteUser(String userId);

    List<UserResponse> getUsers();

    UserResponse getUser(String id);

    UserResponse getMyInfo();

    void createPassword(PasswordCreationRequest request);
}
