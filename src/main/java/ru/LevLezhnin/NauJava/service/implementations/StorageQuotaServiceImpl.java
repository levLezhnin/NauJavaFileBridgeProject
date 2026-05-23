package ru.LevLezhnin.NauJava.service.implementations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.LevLezhnin.NauJava.dto.storageQuotas.StorageQuotaResponseDto;
import ru.LevLezhnin.NauJava.exceptions.common.EntityNotFoundException;
import ru.LevLezhnin.NauJava.exceptions.StorageQuotaExceededException;
import ru.LevLezhnin.NauJava.model.StorageQuota;
import ru.LevLezhnin.NauJava.properties.QuotaProperties;
import ru.LevLezhnin.NauJava.repository.jpa.StorageQuotaRepository;
import ru.LevLezhnin.NauJava.service.base.AbstractStorageQuotaService;
import ru.LevLezhnin.NauJava.utils.RequestContextService;

import java.time.Instant;

@Service
public class StorageQuotaServiceImpl extends AbstractStorageQuotaService {

    private static final Logger log = LoggerFactory.getLogger(StorageQuotaServiceImpl.class);

    private final StorageQuotaRepository storageQuotaRepository;
    private final RequestContextService requestContextService;

    @Autowired
    public StorageQuotaServiceImpl(QuotaProperties quotaProperties, StorageQuotaRepository storageQuotaRepository, RequestContextService requestContextService) {
        super(quotaProperties);
        this.storageQuotaRepository = storageQuotaRepository;
        this.requestContextService = requestContextService;
    }

    private StorageQuota getEntityByIdWithLock(Long storageQuotaId) {
        return storageQuotaRepository.findForUpdateById(storageQuotaId)
                .orElseThrow(() -> new EntityNotFoundException("Квота с id: %d не найдена".formatted(storageQuotaId)));
    }

    @Override
    @Transactional
    public void updateStorageQuota(Long storageQuotaId, long deltaBytes) {
        StorageQuota storageQuota = getEntityByIdWithLock(storageQuotaId);

        long currentUsedStorageBytes = storageQuota.getUsedStorageBytes();
        long newUsedStorageBytes = currentUsedStorageBytes + deltaBytes;
        long maxStorageBytes = storageQuota.getMaxStorageBytes();

        if (newUsedStorageBytes < 0L) {
            log.error("Попытка установить отрицательное значение заполненности. ID квоты: {}, Запрошено байт: {}, Лимит байт: {}, Заполнено байт: {}, Осталось байт: {}", storageQuotaId, deltaBytes, maxStorageBytes, currentUsedStorageBytes, maxStorageBytes - currentUsedStorageBytes);
            newUsedStorageBytes = 0L;
        }

        if (maxStorageBytes < newUsedStorageBytes) {
            log.warn("Превышение квоты хранилища. ID квоты: {}, Запрошено байт: {}, Лимит байт: {}, Осталось байт: {}", storageQuotaId, deltaBytes, maxStorageBytes, maxStorageBytes - currentUsedStorageBytes);
            throw new StorageQuotaExceededException("Превышен лимит хранилища: %d байт из %d возможных".formatted(newUsedStorageBytes, maxStorageBytes));
        }

        storageQuota.setUsedStorageBytes(newUsedStorageBytes);
        storageQuota.setUpdatedAt(Instant.now());
        storageQuotaRepository.save(storageQuota);
    }

    @Override
    public StorageQuotaResponseDto getCurrentUserStorageQuota() {
        Long userId = requestContextService.getUserId();
        StorageQuota storageQuota = storageQuotaRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Квота для пользователя с id: " + userId + " не найдена"));
        return new StorageQuotaResponseDto(
                storageQuota.getUsedStorageBytes().toString(),
                storageQuota.getMaxStorageBytes().toString()
        );
    }
}
