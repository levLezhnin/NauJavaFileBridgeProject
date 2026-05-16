package ru.LevLezhnin.NauJava.dto.storageQuotas;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StorageQuotaResponseDto(
        @JsonProperty("used_storage_bytes") String usedStorageBytes,
        @JsonProperty("max_storage_bytes") String maxStorageBytes) {}
