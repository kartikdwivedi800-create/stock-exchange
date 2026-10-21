package com.nexusexchange.user;

import com.nexusexchange.testsupport.PostgresTestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Full application context against a Testcontainers PostgreSQL; skipped when Docker is not available. */
@SpringBootTest
@Import(PostgresTestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class UserServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
