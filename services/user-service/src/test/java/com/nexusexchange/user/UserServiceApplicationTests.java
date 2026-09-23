package com.nexusexchange.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Starts the full application against a throwaway PostgreSQL container.
 * Skipped automatically when Docker is not available (e.g. on a laptop without Docker Desktop running).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class UserServiceApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void contextLoads() {
    }
}
