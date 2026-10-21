package com.nexusexchange.testsupport;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Repository/JPA test against a real PostgreSQL (Testcontainers):
 * <ul>
 *   <li>loads only the JPA layer ({@link DataJpaTest}); each test runs in a transaction that is rolled back,</li>
 *   <li>keeps the real database instead of swapping in H2 ({@code replace = NONE}),</li>
 *   <li>runs the service's Flyway migrations, and Hibernate {@code ddl-auto: validate} checks every entity,</li>
 *   <li>is skipped (not failed) when Docker is not available.</li>
 * </ul>
 * Usage: annotate a test class with {@code @PostgresRepositoryTest}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresTestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
public @interface PostgresRepositoryTest {
}
