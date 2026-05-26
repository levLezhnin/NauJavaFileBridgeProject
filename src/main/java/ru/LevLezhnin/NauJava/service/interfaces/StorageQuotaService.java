package ru.LevLezhnin.NauJava.service.interfaces;

import ru.LevLezhnin.NauJava.dto.storageQuotas.StorageQuotaResponseDto;
import ru.LevLezhnin.NauJava.model.QuotaTariffs;
import ru.LevLezhnin.NauJava.model.StorageQuota;

/**
 * Сервис управления квотами хранения пользователей.
 * <p>
 * Отвечает за:
 * <ul>
 *   <li>Создание квот по тарифам</li>
 *   <li>Обновление использованного объёма при загрузке/удалении файлов</li>
 *   <li>Получение текущей квоты пользователя</li>
 * </ul>
 *
 * @author Лев Лежнин
 * @see QuotaTariffs
 */
public interface StorageQuotaService {

    /**
     * Создаёт builder для новой квоты согласно указанному тарифу из конфигурации.
     *
     * @param tariff тариф (на данный момент только BASIC)
     * @return builder с уже заполненным maxStorageBytes
     */
    StorageQuota.Builder getQuotaBuilder(QuotaTariffs tariff);

    /**
     * Атомарно изменяет использованный объём хранилища.
     * <p>
     * <b>Важный контракт реализации:</b>
     * <ul>
     *   <li>Использует PESSIMISTIC_WRITE lock на строке квоты</li>
     *   <li>При превышении лимита - {@link ru.LevLezhnin.NauJava.exception.storagequotas.StorageQuotaExceededException}</li>
     *   <li>Отрицательное значение после обновления принудительно приводится к 0</li>
     *   <li>Обновляет поле updatedAt</li>
     * </ul>
     * Вызывается при загрузке (+size) и удалении (-size) файлов.
     *
     * @param storageQuotaId идентификатор квоты
     * @param deltaBytes     дельта (может быть отрицательной)
     */
    void updateStorageQuota(Long storageQuotaId, long deltaBytes);

    /**
     * Возвращает текущую квоту аутентифицированного пользователя.
     */
    StorageQuotaResponseDto getCurrentUserStorageQuota();
}
