package ru.LevLezhnin.NauJava.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.LevLezhnin.NauJava.dto.storageQuotas.StorageQuotaResponseDto;
import ru.LevLezhnin.NauJava.model.StorageQuota;
import ru.LevLezhnin.NauJava.repository.jpa.StorageQuotaRepository;
import ru.LevLezhnin.NauJava.service.interfaces.StorageQuotaService;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/quotas")
public class StorageQuotaController {

    private final StorageQuotaService storageQuotaService;

    @Autowired
    public StorageQuotaController(StorageQuotaService storageQuotaService) {
        this.storageQuotaService = storageQuotaService;
    }

    @GetMapping("/my")
    public StorageQuotaResponseDto getCurrentUserStorageQuota() {
        return storageQuotaService.getCurrentUserStorageQuota();
    }
}
