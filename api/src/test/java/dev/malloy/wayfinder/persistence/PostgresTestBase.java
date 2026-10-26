package dev.malloy.wayfinder.persistence;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * One Postgres container, shared by every test that needs a database.
 *
 * ADR-0001 rejected an embedded database for tests. The CHECK constraints in V1 and the
 * ordering guarantees the catalog relies on are Postgres behaviour, and an embedded engine
 * that approximates them would make these tests agree with each other while disagreeing
 * with production.
 *
 * Started once per JVM rather than once per class. Testcontainers' own reaper stops it at
 * exit, so there is no shutdown hook to get wrong, and the container start cost is paid a
 * single time across the whole suite.
 *
 * These tests require a running Docker daemon. That is a deliberate consequence of the
 * ADR, and they fail rather than skipping when Docker is absent, because a suite that
 * quietly skips its only real database coverage reports a green build it has not earned.
 *
 * Flyway is imported explicitly. The @DataJpaTest slice in Spring Boot 4 pulls in only the
 * JPA repository and Hibernate auto-configurations, so without this the container starts,
 * the schema is never created, and every query fails on a missing table.
 */
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
abstract class PostgresTestBase {

    // Not PostgreSQLContainer<?>: Testcontainers 2.x dropped the self-type generic that
    // the 1.x builder API needed.
    static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
