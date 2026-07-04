package skinemsya.vse.ru.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class LiquibaseMigrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("skinemsya")
            .withUsername("skinemsya")
            .withPassword("skinemsya");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyFoundationMigrations() {
        assertThat(countChangeset("20250615-001")).isEqualTo(1);
        assertThat(countChangeset("20250616-001")).isEqualTo(1);
        assertThat(countChangeset("20250616-002")).isEqualTo(1);
        assertThat(countChangeset("20250628-001")).isEqualTo(1);
        assertThat(countChangeset("20250628-002")).isEqualTo(1);
    }

    private Integer countChangeset(String id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog WHERE id = ?",
                Integer.class,
                id
        );
    }
}
