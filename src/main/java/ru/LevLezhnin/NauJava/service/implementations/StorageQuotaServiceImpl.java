package ru.LevLezhnin.NauJava.service.implementations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.LevLezhnin.NauJava.exceptions.EntityNotFoundException;
import ru.LevLezhnin.NauJava.model.StorageQuota;
import ru.LevLezhnin.NauJava.properties.QuotaProperties;
import ru.LevLezhnin.NauJava.repository.jpa.StorageQuotaRepository;
import ru.LevLezhnin.NauJava.service.base.AbstractStorageQuotaService;

@Service
public class StorageQuotaServiceImpl extends AbstractStorageQuotaService {

    private final StorageQuotaRepository storageQuotaRepository;

    @Autowired
    public StorageQuotaServiceImpl(QuotaProperties quotaProperties, StorageQuotaRepository storageQuotaRepository) {
        super(quotaProperties);
        this.storageQuotaRepository = storageQuotaRepository;
    }

    @Override
    public StorageQuota getUserStorageQuota(Long userId) {
        return storageQuotaRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Квота для пользователя с id: " + userId + " не найдена"));
    }
}
