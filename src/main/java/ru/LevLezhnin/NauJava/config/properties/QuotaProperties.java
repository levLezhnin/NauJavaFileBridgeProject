package ru.LevLezhnin.NauJava.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import ru.LevLezhnin.NauJava.model.QuotaTariffs;

import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурация тарифов квот хранения (app.quotas.*).
 *
 * @author Лев Лежнин
 */
@Configuration
@ConfigurationProperties(prefix = "app.quotas")
public class QuotaProperties {

    private Map<QuotaTariffs, Tariff> tariffs = new HashMap<>();

    public Map<QuotaTariffs, Tariff> getTariffs() {
        return tariffs;
    }

    public void setTariffs(Map<QuotaTariffs, Tariff> tariffs) {
        this.tariffs = tariffs;
    }

    /**
     * Настройки конкретного тарифа квот хранения.
     * <p>
     * Используется в конфигурации {@code app.quotas.tariffs.*}.
     * Содержит два ключевых ограничения:
     * <ul>
     *   <li>{@code maxStorageSize} - максимальный общий объём хранилища для пользователя данного тарифа</li>
     *   <li>{@code maxFileSize} - максимальный размер одного загружаемого файла</li>
     * </ul>
     * Эти значения используются в {@link ru.LevLezhnin.NauJava.service.base.AbstractStorageQuotaService}
     * при создании {@link ru.LevLezhnin.NauJava.model.StorageQuota}.
     *
     * @author Лев Лежнин
     */
    public static class Tariff {

        /** Максимальный общий объём хранилища пользователя */
        private DataSize maxStorageSize;

        /** Максимальный размер одного файла */
        private DataSize maxFileSize;

        public DataSize getMaxStorageSize() {
            return maxStorageSize;
        }

        public void setMaxStorageSize(DataSize maxStorageSize) {
            this.maxStorageSize = maxStorageSize;
        }

        public DataSize getMaxFileSize() {
            return maxFileSize;
        }

        public void setMaxFileSize(DataSize maxFileSize) {
            this.maxFileSize = maxFileSize;
        }
    }
}
