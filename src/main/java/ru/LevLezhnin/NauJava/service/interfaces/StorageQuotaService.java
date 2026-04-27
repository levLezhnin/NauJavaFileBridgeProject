package ru.LevLezhnin.NauJava.service.interfaces;

import ru.LevLezhnin.NauJava.model.QuotaTariffs;
import ru.LevLezhnin.NauJava.model.StorageQuota;

public interface StorageQuotaService {
    StorageQuota.Builder getQuotaBuilder(QuotaTariffs tariff);
    void updateStorageQuota(Long storageQuotaId, Long usedStorageBytes);
    StorageQuota getUserStorageQuota(Long userId);
}
