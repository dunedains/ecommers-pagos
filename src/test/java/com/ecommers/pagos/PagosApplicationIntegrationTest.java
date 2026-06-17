package com.ecommers.pagos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Levanta el contexto completo contra un PostgreSQL real (Testcontainers),
 * ejecutando las migraciones Flyway y validando el mapeo JPA.
 */
@SpringBootTest
@Testcontainers
class PagosApplicationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void contextLoads() {
    }
}
