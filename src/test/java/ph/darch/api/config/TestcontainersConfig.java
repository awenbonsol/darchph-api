package ph.darch.api.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared, reusable Testcontainers Postgres definition (see {@code plans/TASK_7.md}).
 *
 * <p>By default no Spring container starts a shared Postgres — the
 * {@code MigrationTest} manages its own container directly. A bean is offered here so
 * any {@code @SpringBootTest} repository/integration test can {@code @Import} this
 * configuration and bind its datasource to the container with:
 *
 * <pre>{@code
 * @DynamicPropertySource
 * static void props(DynamicPropertyRegistry registry) {
 *     registry.add("spring.datasource.url", postgres::getJdbcUrl);
 *     registry.add("spring.datasource.username", postgres::getUsername);
 *     registry.add("spring.datasource.password", postgres::getPassword);
 * }
 * }</pre>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean(destroyMethod = "")
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withUsername("test")
                .withPassword("test")
                .withDatabaseName("test");
    }
}
