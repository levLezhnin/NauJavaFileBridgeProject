package ru.LevLezhnin.NauJava.service.implementations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.LevLezhnin.NauJava.exceptions.EntityNotFoundException;
import ru.LevLezhnin.NauJava.exceptions.StorageQuotaExceededException;
import ru.LevLezhnin.NauJava.model.StorageQuota;
import ru.LevLezhnin.NauJava.properties.QuotaProperties;
import ru.LevLezhnin.NauJava.repository.jpa.StorageQuotaRepository;
import ru.LevLezhnin.NauJava.service.base.AbstractStorageQuotaService;

@Service
public class StorageQuotaServiceImpl extends AbstractStorageQuotaService {

    private static final Logger log = LoggerFactory.getLogger(StorageQuotaServiceImpl.class);

    private final StorageQuotaRepository storageQuotaRepository;

    @Autowired
    public StorageQuotaServiceImpl(QuotaProperties quotaProperties, StorageQuotaRepository storageQuotaRepository) {
        super(quotaProperties);
        this.storageQuotaRepository = storageQuotaRepository;
    }

    private StorageQuota getEntityById(Long storageQuotaId) {
        return storageQuotaRepository.findById(storageQuotaId)
                .orElseThrow(() -> new EntityNotFoundException("Квота с id: %d не найдена".formatted(storageQuotaId)));
    }

    @Override
    @Transactional
    public void updateStorageQuota(Long storageQuotaId, Long usedStorageBytes) {
        StorageQuota storageQuota = getEntityById(storageQuotaId);

        if (usedStorageBytes > storageQuota.getMaxStorageBytes()) {
            log.warn("Превышение квоты хранилища. ID квоты: {}, Запрошено байт: {}, Лимит байт: {}", storageQuotaId, usedStorageBytes, storageQuota.getMaxStorageBytes());
            throw new StorageQuotaExceededException("Превышен лимит хранилища: %d байт из %d".formatted(usedStorageBytes, storageQuota.getMaxStorageBytes()));
        }

        storageQuota.setUsedStorageBytes(usedStorageBytes);
        storageQuotaRepository.save(storageQuota);
    }

    @Override
    public StorageQuota getUserStorageQuota(Long userId) {
        return storageQuotaRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Квота для пользователя с id: " + userId + " не найдена"));
    }
}
