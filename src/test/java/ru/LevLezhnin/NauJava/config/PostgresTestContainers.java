package ru.LevLezhnin.NauJava.config;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import ru.LevLezhnin.NauJava.constants.ContainerVersionConstants;

public class PostgresTestContainers implements BeforeAllCallback, BeforeEachCallback {

    private static final String DB_NAME = "test_db";
    private static final String USER = "test_user";
    private static final String PASSWORD = "test_password";

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse(ContainerVersionConstants.POSTGRES_CONTAINER_VERSION)
                    .asCompatibleSubstituteFor("postgres")
    )
            .withDatabaseName(DB_NAME)
            .withUsername(USER)
            .withPassword(PASSWORD)
            .withReuse(true);

    @Override
    public void beforeAll(ExtensionContext context) {
        postgres.start();

        // Регистрируем свойства через System.setProperty для надёжности
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgres.getUsername());
        System.setProperty("spring.datasource.password", postgres.getPassword());
        System.setProperty("spring.flyway.enabled", "true");
        System.setProperty("spring.flyway.locations", "classpath:db/migration");
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        runFlywayMigrate();
    }

    @DynamicPropertySource
    public static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        if (postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("spring.flyway.enabled", () -> "true");
            registry.add("spring.flyway.locations", () -> "classpath:db/migration");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        }
    }

    /**
     * Запускает миграции Flyway программно
     */
    private void runFlywayMigrate() {
        try {
            org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .cleanDisabled(false)
                    .load();
            flyway.clean();
            flyway.migrate();
        } catch (Exception e) {
            throw new RuntimeException("Не удалось выполнить миграции Flyway", e);
        }
    }

    public static PostgreSQLContainer<?> getContainer() {
        return postgres;
    }

    public static String getJdbcUrl() {
        return postgres.isRunning() ? postgres.getJdbcUrl() : null;
    }

    public static String getUsername() {
        return postgres.getUsername();
    }

    public static String getPassword() {
        return postgres.getPassword();
    }
}
