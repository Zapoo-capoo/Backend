package com.capoo.identity.configuration;

import com.capoo.identity.constant.PredefinedRole;
import com.capoo.identity.dto.request.UserProfileCreationRequest;
import com.capoo.identity.entity.Role;
import com.capoo.identity.entity.User;
import com.capoo.identity.mapper.ProfileMapper;
import com.capoo.identity.repository.RoleRepository;
import com.capoo.identity.repository.UserRepository;
import com.capoo.identity.repository.httpClient.profileClient.ProfileClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ApplicationInitConfig {
    private final PasswordEncoder passwordEncoder;
    private final ProfileClient profileClient;
    private final ProfileMapper profileMapper;

    @Bean
    public ApplicationRunner applicationRunner(RoleRepository roleRepository, UserRepository userRepository) {
        log.info("Initializing application.....");
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                roleRepository.save(Role.builder()
                        .name(PredefinedRole.USER_ROLE)
                        .description("User role")
                        .build());

                Role adminRole = roleRepository.save(Role.builder()
                        .name(PredefinedRole.ADMIN_ROLE)
                        .description("Admin role")
                        .build());
                var roles = new HashSet<Role>();
                roles.add(adminRole);
                User user = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin"))
                        .roles(roles)
                        .build();
                userRepository.save(user);
                //create profile for admin user
                UserProfileCreationRequest userProfile= profileMapper.toUserProfileCreationRequest(user);
                userProfile.setUserId(user.getId());
                userProfile.setFirstName("admin");
                userProfile.setLastName("admin");

                log.warn("admin user has been created with default password: admin, please change it");
            }
            log.info("Application initialization completed .....");
        };
    }
}
