package ru.LevLezhnin.NauJava.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.LevLezhnin.NauJava.model.StorageQuota;
import ru.LevLezhnin.NauJava.repository.jpa.StorageQuotaRepository;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/quotas")
public class StorageQuotaController {

    private final StorageQuotaRepository storageQuotaRepository;

    @Autowired
    public StorageQuotaController(StorageQuotaRepository storageQuotaRepository) {
        this.storageQuotaRepository = storageQuotaRepository;
    }

    @GetMapping("/user/{userId}")
    public Optional<StorageQuota> getStorageQuotaByUserId(@PathVariable("userId") Long userId) {
        return storageQuotaRepository.findByUserId(userId);
    }
}
