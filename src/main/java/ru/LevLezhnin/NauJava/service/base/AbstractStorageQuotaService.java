package ru.LevLezhnin.NauJava.service.base;

import ru.LevLezhnin.NauJava.model.QuotaTariffs;
import ru.LevLezhnin.NauJava.model.StorageQuota;
import ru.LevLezhnin.NauJava.properties.QuotaProperties;
import ru.LevLezhnin.NauJava.service.interfaces.StorageQuotaService;

public abstract class AbstractStorageQuotaService implements StorageQuotaService {

    private final QuotaProperties quotaProperties;

    public AbstractStorageQuotaService(QuotaProperties quotaProperties) {
        this.quotaProperties = quotaProperties;
    }

    @Override
    public StorageQuota.Builder getQuotaBuilder(QuotaTariffs tariff) {
        QuotaProperties.Tariff quotaTariff = quotaProperties.getTariffs().get(tariff);

        if (quotaTariff == null) {
            throw new IllegalArgumentException("Не задана конфигурация для тарифа: " + tariff);
        }

        return StorageQuota.builder()
                .setMaxStorageBytes(quotaTariff.getMaxStorageSize().toBytes())
                .setUsedStorageBytes(0L);
    }
}
