package ru.LevLezhnin.NauJava.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import ru.LevLezhnin.NauJava.model.QuotaTariffs;

import java.util.HashMap;
import java.util.Map;

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

    public static class Tariff {

        private DataSize maxStorageSize;
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
