# test-support

Shared test infrastructure, used by services with `<scope>test</scope>` only.

- `@PostgresRepositoryTest`: `@DataJpaTest` against a real PostgreSQL 16 started by Testcontainers. Runs the
  service's Flyway migrations and Hibernate schema validation; each test is rolled back; skipped when Docker is not running.
- `PostgresTestcontainersConfiguration`: the container bean (`@ServiceConnection`), reusable in `@SpringBootTest`
  via `@Import(PostgresTestcontainersConfiguration.class)`.

Kafka and Redis container configurations are added when the first service uses them (Sprint 4 / Sprint 6).
