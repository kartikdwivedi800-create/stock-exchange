package com.nexusexchange.user.config;

import com.nexusexchange.user.entity.Role;
import com.nexusexchange.user.entity.User;
import com.nexusexchange.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the initial admin account at startup when ADMIN_USERNAME and ADMIN_PASSWORD are set.
 * Idempotent: an existing user with that username is left untouched. Never logs the password.
 */
@Slf4j
@Component
@EnableConfigurationProperties(AdminProperties.class)
public class AdminSeeder implements ApplicationRunner {

    private final AdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(AdminProperties properties, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured()) {
            log.debug("ADMIN_USERNAME / ADMIN_PASSWORD not set; skipping admin seeding");
            return;
        }

        String username = properties.username().trim();
        String email = username + "@nexus.local";
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            log.info("Admin user '{}' already exists; nothing to do", username);
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Cannot seed admin '{}': email {} is already registered", username, email);
            return;
        }

        userRepository.save(User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(properties.password()))
                .firstName("Admin")
                .lastName("User")
                .role(Role.ROLE_ADMIN)
                .build());
        log.info("Admin user '{}' created", username);
    }
}
