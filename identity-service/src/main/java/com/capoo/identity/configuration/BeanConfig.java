package com.capoo.identity.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Standalone bean definitions kept out of SecurityConfig to avoid a circular
 * reference: SecurityConfig depends on CustomJwtDecoder -> AuthenticationService
 * -> UserService -> PasswordEncoder. Defining PasswordEncoder here breaks the cycle.
 */
@Configuration
public class BeanConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
