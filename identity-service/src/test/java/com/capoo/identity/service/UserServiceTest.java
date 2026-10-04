package com.capoo.identity.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.capoo.event.dto.UserProfileCreationRequest;
import com.capoo.identity.dto.request.UserCreationRequest;
import com.capoo.identity.dto.response.UserResponse;
import com.capoo.identity.entity.User;
import com.capoo.identity.exception.AppException;
import com.capoo.identity.httpClient.profileClient.ProfileClient;
import com.capoo.identity.mapper.ProfileMapper;
import com.capoo.identity.mapper.UserMapper;
import com.capoo.identity.repository.RoleRepository;
import com.capoo.identity.repository.UserRepository;

/**
 * Unit tests for the user registration flow ({@link UserService#createUser}).
 * Profile creation now happens synchronously through {@link ProfileClient} (Feign),
 * so we verify the happy path, the validation short-circuits, and the rollback.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProfileClient profileClient;

    @Mock
    private ProfileMapper profileMapper;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private UserService userService;

    private UserCreationRequest request;
    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void initData() {
        LocalDate dob = LocalDate.of(1990, 1, 1);

        request = UserCreationRequest.builder()
                .username("john")
                .password("12345678")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .dob(dob)
                .build();

        user = User.builder()
                .id("cf0600f538b3")
                .username("john")
                .email("john@example.com")
                .build();

        userResponse = UserResponse.builder()
                .id("cf0600f538b3")
                .username("john")
                .email("john@example.com")
                .build();
    }

    @Test
    void createUser_validRequest_success() {
        // GIVEN
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userMapper.toUser(request)).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(roleRepository.findById(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(user);
        when(profileMapper.toUserProfileCreationRequest(request)).thenReturn(new UserProfileCreationRequest());
        when(kafkaTemplate.send(anyString(), any())).thenReturn(new CompletableFuture<>());
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        // WHEN
        var response = userService.createUser(request);

        // THEN
        Assertions.assertThat(response.getId()).isEqualTo("cf0600f538b3");
        Assertions.assertThat(response.getUsername()).isEqualTo("john");
        // profile must be created synchronously via Feign, not via Kafka
        verify(profileClient).createUserProfileForUser(any());
    }

    @Test
    void createUser_usernameExisted_fail() {
        // GIVEN
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // WHEN
        var exception = assertThrows(AppException.class, () -> userService.createUser(request));

        // THEN
        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1002); // USER_EXISTED
        verify(userRepository, never()).save(any());
        verify(profileClient, never()).createUserProfileForUser(any());
    }

    @Test
    void createUser_emailExisted_fail() {
        // GIVEN
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // WHEN
        var exception = assertThrows(AppException.class, () -> userService.createUser(request));

        // THEN
        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1011); // EMAIL_EXISTED
        verify(userRepository, never()).save(any());
        verify(profileClient, never()).createUserProfileForUser(any());
    }

    @Test
    void createUser_profileCreationFails_rollsBackUser() {
        // GIVEN
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userMapper.toUser(request)).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(roleRepository.findById(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(user);
        when(profileMapper.toUserProfileCreationRequest(request)).thenReturn(new UserProfileCreationRequest());
        when(profileClient.createUserProfileForUser(any())).thenThrow(new RuntimeException("profile-service down"));

        // WHEN
        var exception = assertThrows(AppException.class, () -> userService.createUser(request));

        // THEN
        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1012); // PROFILE_CREATION_FAILED
        // the created user must be rolled back
        verify(userRepository).deleteById(user.getId());
        // no welcome notification should be published when registration fails
        verify(kafkaTemplate, never()).send(anyString(), any());
    }
}
