package ru.LevLezhnin.NauJava.service.interfaces;

import ru.LevLezhnin.NauJava.model.QuotaTariffs;
import ru.LevLezhnin.NauJava.model.StorageQuota;

public interface StorageQuotaService {
    StorageQuota.Builder getQuotaBuilder(QuotaTariffs tariff);
    StorageQuota getUserStorageQuota(Long userId);
}
