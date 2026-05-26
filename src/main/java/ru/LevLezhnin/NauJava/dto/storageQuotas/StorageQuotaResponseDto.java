package ru.LevLezhnin.NauJava.dto.storageQuotas;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO возврата данных о квоте пользователя
 * @param usedStorageBytes  количество занятых пользователем байт
 * @param maxStorageBytes   максимальное доступное пользователю пространство в байтах
 * @author Лев Лежнин
 */
public record StorageQuotaResponseDto(
        @JsonProperty("used_storage_bytes") String usedStorageBytes,
        @JsonProperty("max_storage_bytes") String maxStorageBytes) {}
