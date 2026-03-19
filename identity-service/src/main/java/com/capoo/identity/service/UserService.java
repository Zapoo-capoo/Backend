package com.capoo.identity.service;

import com.capoo.identity.constant.PredefinedRole;
import com.capoo.identity.dto.request.PasswordCreationRequest;
import com.capoo.identity.dto.request.UserCreationRequest;
import com.capoo.identity.dto.request.UserProfileCreationRequest;
import com.capoo.identity.dto.request.UserUpdateRequest;
import com.capoo.identity.dto.response.UserResponse;
import com.capoo.identity.entity.Role;
import com.capoo.identity.entity.User;
import com.capoo.identity.exception.AppException;
import com.capoo.identity.mapper.ProfileMapper;
import com.capoo.identity.mapper.UserMapper;
import com.capoo.identity.repository.RoleRepository;
import com.capoo.identity.repository.UserRepository;
import com.capoo.identity.repository.httpClient.profileClient.ProfileClient;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletRequestAttributeEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.capoo.identity.exception.ErrorCode;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
    PasswordEncoder passwordEncoder;
    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    ProfileClient profileClient;
    ProfileMapper profileMapper;
    public UserResponse createUser(UserCreationRequest userCreationRequest) {
        //Check if user exist
        if (userRepository.existsByUsername(userCreationRequest.getUsername())) throw new AppException(ErrorCode.USER_EXISTED);
        User user = userMapper.toUser(userCreationRequest);
        //Create user
        user.setPassword(passwordEncoder.encode(userCreationRequest.getPassword()));
        HashSet<Role> roles=new HashSet<>();
        roleRepository.findById(PredefinedRole.USER_ROLE).ifPresent(roles::add);
        user.setRoles(roles);
        user= userRepository.save(user);
        //Create profile for user
        UserProfileCreationRequest userProfile= profileMapper.toUserProfileCreationRequest(userCreationRequest);
        userProfile.setUserId(user.getId());
        ServletRequestAttributes attributes = (ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        var header= attributes.getRequest().getHeader("Authorization");

        profileClient.createUserProfileForUser(userProfile);
        return userMapper.toUserResponse(user);
    }
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        userMapper.updateUser(user, request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        var roles = roleRepository.findAllById(request.getRoles());
        user.setRoles(new HashSet<>(roles));

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getUsers() {
        log.info("In method get Users");
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUser(String id) {
        return userMapper.toUserResponse(
                userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }
    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();
        User user = userRepository.findByUsername(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        var userResponse = userMapper.toUserResponse(user);
        userResponse.setNoPassword(!StringUtils.hasText(user.getPassword()));
        return userResponse;
    }
    public void createPassword(PasswordCreationRequest request){
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByUsername(name).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (StringUtils.hasText(user.getPassword()))
            throw new AppException(ErrorCode.PASSWORD_EXISTED);

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
    }
}
