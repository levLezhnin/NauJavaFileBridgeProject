package ru.LevLezhnin.NauJava.model;

import ru.LevLezhnin.NauJava.config.DataSizeConstants;

public enum QuotaTariffs {
    BASIC(1 * DataSizeConstants.GB);

    private final long maxStorageBytes;

    public long getMaxStorageBytes() {
        return maxStorageBytes;
    }

    public StorageQuota.Builder getBasicQuotaBuilder() {
        return StorageQuota.builder()
                .setMaxStorageBytes(getMaxStorageBytes())
                .setUsedStorageBytes(0L);
    }

    QuotaTariffs(long maxStorageBytes) {
        this.maxStorageBytes = maxStorageBytes;
    }

}
