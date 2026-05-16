package ru.LevLezhnin.NauJava.service.interfaces;

import ru.LevLezhnin.NauJava.dto.storageQuotas.StorageQuotaResponseDto;
import ru.LevLezhnin.NauJava.model.QuotaTariffs;
import ru.LevLezhnin.NauJava.model.StorageQuota;

public interface StorageQuotaService {
    StorageQuota.Builder getQuotaBuilder(QuotaTariffs tariff);
    /**
     * Обновляет usedStorageBytes на deltaBytes
     * @param storageQuotaId id квоты
     * @param deltaBytes дельта изменения количества задействованных байт
     */
    void updateStorageQuota(Long storageQuotaId, long deltaBytes);
    StorageQuotaResponseDto getCurrentUserStorageQuota();
}
